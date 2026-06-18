package com.example.authtutorial.common.adapter.web;

import com.example.authtutorial.common.domain.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 共用的「驅動轉接器」橫切關注點：把 domain 例外轉成 HTTP 回應。
 *
 * <p>domain 不認得 HTTP；由最外圈的轉接器負責翻譯。這裡用 RFC 9457 的
 * {@link ProblemDetail} 統一錯誤格式。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex) {
        // 業務規則違反 → 400 Bad Request（含可讀訊息）。
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("業務規則違反");
        return problem;
    }
}
