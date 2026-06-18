package com.example.authtutorial.saml.domain.port.out;

import com.example.authtutorial.saml.domain.model.SamlAssertion;

/**
 * 被驅動端埠：驗證 SAML 斷言的數位簽章，並解析出 {@link SamlAssertion}。
 *
 * <p>真實世界以 XML 數位簽章 (XML-DSig) 實作（如 OpenSAML / Spring Security SAML）；
 * 本教學的轉接器以簡化的「HMAC 簽章」示範相同的「先驗簽章、再驗條件」邊界。</p>
 */
public interface AssertionSignatureVerifierPort {

    /**
     * @throws com.example.authtutorial.common.domain.DomainException 簽章無效或格式錯誤。
     */
    SamlAssertion verifyAndParse(String rawAssertion);
}
