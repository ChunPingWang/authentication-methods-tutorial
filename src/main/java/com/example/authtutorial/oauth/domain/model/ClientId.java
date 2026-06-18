package com.example.authtutorial.oauth.domain.model;

import com.example.authtutorial.common.domain.DomainException;

/**
 * 值物件：OAuth 客戶端 (Client) 的識別碼，也就是「想存取資源的 App A」。
 */
public record ClientId(String value) {

    public ClientId {
        if (value == null || value.isBlank()) {
            throw new DomainException("ClientId 不可為空白");
        }
    }

    public static ClientId of(String value) {
        return new ClientId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
