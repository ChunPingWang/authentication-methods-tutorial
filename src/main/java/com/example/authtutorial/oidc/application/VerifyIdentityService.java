package com.example.authtutorial.oidc.application;

import com.example.authtutorial.oidc.domain.model.IdTokenClaims;
import com.example.authtutorial.oidc.domain.model.VerifiedIdentity;
import com.example.authtutorial.oidc.domain.port.in.VerifyIdentityUseCase;
import com.example.authtutorial.oidc.domain.port.out.IdTokenParserPort;

import java.time.Clock;

/**
 * 應用服務：編排 OIDC 身分驗證流程。
 *
 * <p>步驟對應圖中 OIDC：①使用者認證 → ②IdP 發 ID Token → ③App 驗證並萃取身分。
 * 這裡負責 ③：先用埠驗簽章，再交給 domain 驗 claims。</p>
 */
public class VerifyIdentityService implements VerifyIdentityUseCase {

    private final IdTokenParserPort idTokenParser;
    private final Clock clock;
    private final String trustedIssuer;

    public VerifyIdentityService(IdTokenParserPort idTokenParser, Clock clock, String trustedIssuer) {
        this.idTokenParser = idTokenParser;
        this.clock = clock;
        this.trustedIssuer = trustedIssuer;
    }

    @Override
    public VerifiedIdentity verify(VerifyIdTokenCommand command) {
        // ① 被驅動端：驗證簽章並取出 claims（不可信 → 可信的轉折點）。
        IdTokenClaims claims = idTokenParser.parseAndVerifySignature(command.rawIdToken());

        // ② domain：驗證業務規則（過期 / 發行者 / 受眾 / nonce）並萃取身分。
        return claims.validate(clock.instant(), trustedIssuer,
                command.expectedAudience(), command.expectedNonce());
    }
}
