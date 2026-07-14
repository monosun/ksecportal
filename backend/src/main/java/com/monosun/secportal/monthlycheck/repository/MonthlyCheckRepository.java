package com.monosun.secportal.monthlycheck.repository;

import com.monosun.secportal.monthlycheck.entity.MonthlyCheckItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MonthlyCheckRepository extends JpaRepository<MonthlyCheckItem, Long> {

    List<MonthlyCheckItem> findByYearMonthOrderBySortOrderAscIdAsc(String yearMonth);

    @Query("SELECT DISTINCT m.yearMonth FROM MonthlyCheckItem m ORDER BY m.yearMonth DESC")
    List<String> findDistinctYearMonths();

    /** 주어진 월보다 이전(문자열=연대순)이면서 점검 항목이 존재하는 월들을 최신순으로 조회 */
    @Query("SELECT DISTINCT m.yearMonth FROM MonthlyCheckItem m WHERE m.yearMonth < :yearMonth ORDER BY m.yearMonth DESC")
    List<String> findYearMonthsBefore(String yearMonth);
}
