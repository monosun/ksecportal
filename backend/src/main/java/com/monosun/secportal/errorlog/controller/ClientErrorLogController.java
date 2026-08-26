package com.monosun.secportal.errorlog.controller;

import com.monosun.secportal.common.response.ApiResponse;
import com.monosun.secportal.errorlog.dto.ErrorLogDto;
import com.monosun.secportal.errorlog.service.ErrorLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 화면(JS)에서 발생한 오류를 올려 받는 창구.
 * 로그인한 사용자면 누구나 호출할 수 있으나 조회·관리는 관리자만 가능하다.
 */
@RestController
@RequestMapping("/error-logs")
@RequiredArgsConstructor
public class ClientErrorLogController {

    private final ErrorLogService errorLogService;

    @PostMapping("/client")
    public ApiResponse<Void> report(@RequestBody ErrorLogDto.ClientReport report) {
        errorLogService.recordClient(report);
        return ApiResponse.noContent();
    }
}
