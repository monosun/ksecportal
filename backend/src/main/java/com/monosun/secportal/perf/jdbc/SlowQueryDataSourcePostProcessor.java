package com.monosun.secportal.perf.jdbc;

import com.monosun.secportal.perf.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * DataSource → Connection → Statement 를 동적 프록시로 감싸 SQL 실행 시간을 잰다.
 * 임계시간을 넘긴 구문만 성능관리에 기록하며, 기록 자체가 만드는 SQL은 제외한다.
 *
 * 별도 라이브러리 없이 JDK 프록시만 사용하고, 위임 외에는 아무것도 바꾸지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlowQueryDataSourcePostProcessor implements BeanPostProcessor {

    /** PerformanceService 는 DataSource 에 의존하므로 지연 조회로 순환 참조를 피한다 */
    private final ObjectProvider<PerformanceService> performanceServiceProvider;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource dataSource && !Proxy.isProxyClass(bean.getClass())) {
            return Proxy.newProxyInstance(
                    DataSource.class.getClassLoader(),
                    new Class<?>[]{DataSource.class},
                    new DataSourceHandler(dataSource));
        }
        return bean;
    }

    // ── 프록시 핸들러 ────────────────────────────────────────────────────────

    private class DataSourceHandler implements InvocationHandler {
        private final DataSource target;

        DataSourceHandler(DataSource target) { this.target = target; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result = call(target, method, args);
            if (result instanceof Connection connection && "getConnection".equals(method.getName())) {
                return Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class<?>[]{Connection.class},
                        new ConnectionHandler(connection));
            }
            return result;
        }
    }

    private class ConnectionHandler implements InvocationHandler {
        private final Connection target;

        ConnectionHandler(Connection target) { this.target = target; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result = call(target, method, args);
            String name = method.getName();
            if (result instanceof Statement statement
                    && ("prepareStatement".equals(name) || "prepareCall".equals(name) || "createStatement".equals(name))) {
                String sql = (args != null && args.length > 0 && args[0] instanceof String s) ? s : null;
                Class<?> iface = result instanceof CallableStatement ? CallableStatement.class
                        : result instanceof PreparedStatement ? PreparedStatement.class
                        : Statement.class;
                return Proxy.newProxyInstance(
                        iface.getClassLoader(),
                        new Class<?>[]{iface},
                        new StatementHandler(statement, sql));
            }
            return result;
        }
    }

    private class StatementHandler implements InvocationHandler {
        private final Statement target;
        private final String preparedSql;

        StatementHandler(Statement target, String preparedSql) {
            this.target = target;
            this.preparedSql = preparedSql;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!method.getName().startsWith("execute")) {
                return call(target, method, args);
            }
            long start = System.nanoTime();
            try {
                return call(target, method, args);
            } finally {
                long durationMs = (System.nanoTime() - start) / 1_000_000L;
                String sql = preparedSql;
                if (sql == null && args != null && args.length > 0 && args[0] instanceof String s) sql = s;
                record(sql, durationMs);
            }
        }
    }

    // ── 공통 ────────────────────────────────────────────────────────────────

    /** 프록시 메서드를 실제 대상에 그대로 위임 (예외는 원래 예외 그대로 던진다) */
    private Object call(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /** 기록 처리 도중 다시 기록 경로로 들어오는 것을 막는 재진입 가드 */
    private static final ThreadLocal<Boolean> RECORDING = ThreadLocal.withInitial(() -> false);

    private void record(String sql, long durationMs) {
        if (RECORDING.get()) return;
        RECORDING.set(true);
        try {
            // 성능 기록을 저장하는 중에 발생한 SQL 은 기록하지 않는다 (자기 자신 되먹임 방지)
            if (PerformanceService.isFlushing()) return;
            if (sql == null || sql.isBlank()) return;
            if (sql.contains("slow_logs")) return;

            PerformanceService service = performanceServiceProvider.getIfAvailable();
            if (service == null) return;
            if (durationMs < service.thresholdMs()) return;
            service.recordSql(sql, durationMs, currentUsername());
        } catch (Exception e) {
            // 성능 수집이 실제 쿼리 실행을 방해하면 안 되므로 삼킨다
            log.debug("SQL 성능 기록 실패: {}", e.getMessage());
        } finally {
            RECORDING.set(false);
        }
    }

    private String currentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
