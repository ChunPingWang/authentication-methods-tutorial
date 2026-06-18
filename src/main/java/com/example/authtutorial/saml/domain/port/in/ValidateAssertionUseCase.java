package com.example.authtutorial.saml.domain.port.in;

import com.example.authtutorial.saml.domain.model.AssertedIdentity;

/**
 * 驅動端埠：驗證 SAML 斷言並取得身分。
 */
public interface ValidateAssertionUseCase {

    /**
     * 對應圖中 SAML：App「validates signed assertion and extracts user identity」。
     *
     * @throws com.example.authtutorial.common.domain.DomainException 簽章或條件驗證失敗。
     */
    AssertedIdentity validate(ValidateAssertionCommand command);

    /**
     * @param rawAssertion     收到的（已簽章）斷言內容。
     * @param expectedAudience 預期受眾（本 Service Provider）。
     */
    record ValidateAssertionCommand(String rawAssertion, String expectedAudience) {
    }
}
