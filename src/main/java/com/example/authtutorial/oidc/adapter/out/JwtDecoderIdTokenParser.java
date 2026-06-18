package com.example.authtutorial.oidc.adapter.out;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.oidc.domain.model.IdTokenClaims;
import com.example.authtutorial.oidc.domain.port.out.IdTokenParserPort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.List;

/**
 * 被驅動轉接器：用 Spring Security 的 {@link JwtDecoder}（底層為 Nimbus）驗證
 * JWT 簽章，並把標準 claims 對應成 domain 的 {@link IdTokenClaims}。
 *
 * <p>這個轉接器刻意「只負責簽章與格式」：時間 / 受眾 / nonce 等業務驗證留給
 * domain（{@link IdTokenClaims#validate}）。因此建構此類別時所使用的
 * {@code JwtDecoder} 會把預設的時間驗證關掉（見組態），讓 domain 擁有完整話語權。</p>
 *
 * <p>同一個轉接器可搭配不同的 {@code JwtDecoder}：
 * <ul>
 *   <li>離線教學：HMAC（共享密鑰）解碼器 → 完全不需外部 IdP。</li>
 *   <li>真實情境：以 Keycloak 的 JWKS URI 建立的解碼器（見整合測試）。</li>
 * </ul>
 */
public class JwtDecoderIdTokenParser implements IdTokenParserPort {

    private final JwtDecoder jwtDecoder;

    public JwtDecoderIdTokenParser(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public IdTokenClaims parseAndVerifySignature(String rawIdToken) {
        try {
            Jwt jwt = jwtDecoder.decode(rawIdToken);
            List<String> audience = jwt.getAudience() == null ? List.of() : jwt.getAudience();
            return new IdTokenClaims(
                    jwt.getIssuer() == null ? null : jwt.getIssuer().toString(),
                    jwt.getSubject(),
                    audience,
                    jwt.getIssuedAt(),
                    jwt.getExpiresAt(),
                    jwt.getClaimAsString("nonce"),
                    jwt.getClaimAsString("email"));
        } catch (JwtException ex) {
            // 把框架例外翻譯成 domain 例外。
            throw new DomainException("ID Token 簽章或格式無效: " + ex.getMessage());
        }
    }
}
