package com.monosun.secportal.common.exception;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.errorlog.entity.ErrorLog;
import com.monosun.secportal.errorlog.service.ErrorLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /** 처리 중 발생한 오류는 관리 > 에러 로그 관리 화면에서 확인할 수 있도록 적재한다. */
    private final ErrorLogService errorLogService;

    @Value("${spring.servlet.multipart.max-file-size:100MB}")
    private String maxFileSize;

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFoundException(ResourceNotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleUnauthorizedException(UnauthorizedException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException e) {
        return ApiResponse.error("Access denied");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ApiResponse.error(message);
    }

    /**
     * 첨부파일 검증 실패(허용되지 않는 확장자 등)는 사용자가 고칠 수 있는 오류이므로
     * 500 "Internal server error" 로 뭉뚱그리지 않고 사유를 그대로 내려준다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        errorLogService.record(ErrorLog.Level.WARN, e, HttpStatus.BAD_REQUEST.value());
        return ApiResponse.error(e.getMessage() != null ? e.getMessage() : "요청 값이 올바르지 않습니다.");
    }

    /** 업로드 용량 초과 — 무엇이 문제인지 알 수 있도록 한도를 함께 알려준다. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResponse<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        errorLogService.record(ErrorLog.Level.WARN, e, HttpStatus.PAYLOAD_TOO_LARGE.value());
        return ApiResponse.error("파일 용량이 허용 한도를 초과했습니다. (최대 " + maxFileSize + ")");
    }

    /** 그 밖의 멀티파트 처리 오류(전송 중단 등) */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMultipart(MultipartException e) {
        log.warn("Multipart 처리 실패: {}", e.getMessage());
        errorLogService.record(ErrorLog.Level.WARN, e, HttpStatus.BAD_REQUEST.value());
        return ApiResponse.error("파일 업로드 처리 중 오류가 발생했습니다. 파일 크기와 네트워크 상태를 확인한 뒤 다시 시도해주세요.");
    }

    /**
     * 없는 경로 요청(오타 링크·헬스체크·스캐너 등)은 서버 장애가 아니므로 404 로 돌려준다.
     * 외부에서 반복 호출되면 에러 로그를 뒤덮으므로 적재하지 않고 서버 로그에만 남긴다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("없는 경로 요청: {}", e.getMessage());
        return ApiResponse.error("요청한 경로를 찾을 수 없습니다.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("Unhandled exception", e);
        errorLogService.record(ErrorLog.Level.ERROR, e, HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ApiResponse.error("Internal server error");
    }
}
