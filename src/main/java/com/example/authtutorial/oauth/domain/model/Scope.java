package com.example.authtutorial.oauth.domain.model;

import com.example.authtutorial.common.domain.DomainException;

/**
 * 值物件：單一「範圍 (scope)」，描述 App 被允許做的<b>一件事</b>，
 * 例如 {@code contacts.read}、{@code calendar.write}。
 *
 * <p>OAuth 的重點是「能存取什麼 (what)」，scope 就是這個答案。</p>
 */
public record Scope(String value) {

    public Scope {
        if (value == null || value.isBlank()) {
            throw new DomainException("Scope 不可為空白");
        }
        value = value.trim();
    }

    public static Scope of(String value) {
        return new Scope(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
