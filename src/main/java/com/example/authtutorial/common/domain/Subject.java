package com.example.authtutorial.common.domain;

import java.util.Objects;

/**
 * 共享核心 (Shared Kernel) 值物件：代表「被驗證的主體」，也就是 OIDC/SAML
 * 中常見的 {@code sub} (subject) 識別碼。
 *
 * <p>值物件 (Value Object) 的特性：
 * <ul>
 *   <li>不可變 (immutable)：建立後不能被修改。</li>
 *   <li>以「值」判斷相等，而非身分 (identity)。</li>
 *   <li>在建構子就保證自身有效 (always-valid)。</li>
 * </ul>
 *
 * <p>用 {@code record} 實作可自動取得不可變性、equals/hashCode。</p>
 *
 * @param value 主體識別碼，例如使用者名稱或唯一 ID，不可為空白。
 */
public record Subject(String value) {

    public Subject {
        if (value == null || value.isBlank()) {
            throw new DomainException("Subject 不可為空白");
        }
        value = value.trim();
    }

    public static Subject of(String value) {
        return new Subject(value);
    }

    @Override
    public String toString() {
        return value;
    }

    /** 明確覆寫以強調：相等性完全由 value 決定（record 預設行為，這裡僅作教學說明）。 */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Subject other && Objects.equals(value, other.value);
    }
}
