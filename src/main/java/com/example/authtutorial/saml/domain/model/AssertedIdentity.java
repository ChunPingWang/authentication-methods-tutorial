package com.example.authtutorial.saml.domain.model;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.common.domain.Subject;

import java.util.Map;

/**
 * 值物件：通過 SAML 斷言驗證後萃取的身分（含屬性）。
 */
public record AssertedIdentity(Subject subject, String issuer, Map<String, String> attributes) {

    public AssertedIdentity(String subject, String issuer, Map<String, String> attributes) {
        this(Subject.of(subject), issuer, attributes);
    }

    public AssertedIdentity {
        if (subject == null) {
            throw new DomainException("已斷言身分必須包含 subject");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new DomainException("已斷言身分必須包含 issuer");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
