package com.example.authtutorial.saml.application;

import com.example.authtutorial.saml.domain.model.AssertedIdentity;
import com.example.authtutorial.saml.domain.model.SamlAssertion;
import com.example.authtutorial.saml.domain.port.in.ValidateAssertionUseCase;
import com.example.authtutorial.saml.domain.port.out.AssertionSignatureVerifierPort;

import java.time.Clock;

/**
 * 應用服務：編排 SAML 斷言驗證流程。
 */
public class ValidateAssertionService implements ValidateAssertionUseCase {

    private final AssertionSignatureVerifierPort signatureVerifier;
    private final Clock clock;
    private final String trustedIssuer;

    public ValidateAssertionService(AssertionSignatureVerifierPort signatureVerifier,
                                    Clock clock, String trustedIssuer) {
        this.signatureVerifier = signatureVerifier;
        this.clock = clock;
        this.trustedIssuer = trustedIssuer;
    }

    @Override
    public AssertedIdentity validate(ValidateAssertionCommand command) {
        // ① 被驅動端：驗證簽章並解析 XML/payload。
        SamlAssertion assertion = signatureVerifier.verifyAndParse(command.rawAssertion());

        // ② domain：驗證 Conditions（時間 / 發行者 / 受眾）並萃取身分。
        return assertion.validate(clock.instant(), trustedIssuer, command.expectedAudience());
    }
}
