package com.example.authtutorial.sso.domain.model;

import com.example.authtutorial.common.domain.DomainException;

import java.util.UUID;

/**
 * 值物件：SSO 單一登入工作階段的識別碼。
 *
 * @param value 不可為空白的字串（通常是 UUID）。
 */
public record SessionId(String value) {

    public SessionId {
        if (value == null || value.isBlank()) {
            throw new DomainException("SessionId 不可為空白");
        }
    }

    /** 產生一個新的、隨機的工作階段 ID。 */
    public static SessionId newId() {
        return new SessionId(UUID.randomUUID().toString());
    }

    public static SessionId of(String value) {
        return new SessionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
