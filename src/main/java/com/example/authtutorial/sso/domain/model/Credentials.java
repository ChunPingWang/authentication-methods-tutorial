package com.example.authtutorial.sso.domain.model;

import com.example.authtutorial.common.domain.DomainException;

/**
 * 值物件：使用者用來「登入一次」的憑證。
 *
 * <p>對應圖中 SSO 流程的第 3 步「User logs in once」。憑證只在登入當下被
 * 身分提供者 (IdP) 使用一次，之後存取其他 App 都改用工作階段，不再重送密碼。</p>
 */
public record Credentials(String username, String password) {

    public Credentials {
        if (username == null || username.isBlank()) {
            throw new DomainException("使用者名稱不可為空白");
        }
        if (password == null || password.isEmpty()) {
            throw new DomainException("密碼不可為空");
        }
    }
}
