package com.example.authtutorial.common.domain;

/**
 * 領域例外：當「業務規則 (invariant)」被違反時拋出。
 *
 * <p>例如：權杖已過期、未取得使用者同意就想發 token、Audience 不符等。
 * 這是 domain 層自己的語言，<b>刻意不依賴</b> Spring 或 HTTP 的概念，
 * 由外層轉接器負責把它轉成對應的 HTTP 狀態碼。</p>
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
