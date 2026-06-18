package com.example.authtutorial.saml.adapter.out;

import com.example.authtutorial.saml.domain.model.SamlAssertion;
import com.example.authtutorial.saml.domain.port.out.AssertionSignatureVerifierPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 被驅動轉接器：以 {@link DemoSamlCodec} 驗證 SAML 斷言簽章並解析。
 */
@Component
public class DemoSignatureVerifierAdapter implements AssertionSignatureVerifierPort {

    private final DemoSamlCodec codec;

    public DemoSignatureVerifierAdapter(@Value("${saml.signing-secret:tutorial-saml-shared-secret}") String secret) {
        this.codec = new DemoSamlCodec(secret);
    }

    @Override
    public SamlAssertion verifyAndParse(String rawAssertion) {
        return codec.verifyAndParse(rawAssertion);
    }
}
