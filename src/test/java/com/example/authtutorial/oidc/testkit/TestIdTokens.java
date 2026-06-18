package com.example.authtutorial.oidc.testkit;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * 測試工具：扮演「IdP 發行 ID Token」的角色，用 HS256 簽出 JWT；同時提供與
 * 正式組態相同的 HMAC {@link JwtDecoder}，讓測試走真實的「簽章驗證」路徑。
 */
public final class TestIdTokens {

    public static final String SECRET = "tutorial-oidc-hmac-secret-key-32bytes!!";
    public static final String ISSUER = "https://idp.tutorial.local";
    public static final String AUDIENCE = "tutorial-app";

    private TestIdTokens() {
    }

    /** 與 {@code BeanConfiguration#idTokenJwtDecoder} 相同：只驗簽章，時間留給 domain。 */
    public static JwtDecoder hmacDecoder() {
        SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
        return decoder;
    }

    /** 以正確密鑰簽發一個 ID Token。 */
    public static String signed(String issuer, String subject, String audience,
                                Instant issuedAt, Instant expiresAt, String nonce, String email) {
        return sign(SECRET, issuer, subject, audience, issuedAt, expiresAt, nonce, email);
    }

    /** 允許指定密鑰（用來製造「簽章錯誤」的情境）。 */
    public static String sign(String secret, String issuer, String subject, String audience,
                              Instant issuedAt, Instant expiresAt, String nonce, String email) {
        try {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(subject)
                    .audience(audience)
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt));
            if (nonce != null) {
                claims.claim("nonce", nonce);
            }
            if (email != null) {
                claims.claim("email", email);
            }
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
            jwt.sign(new MACSigner(padTo32Bytes(secret)));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("無法簽發測試用 ID Token", e);
        }
    }

    private static byte[] padTo32Bytes(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= 32) {
            return bytes;
        }
        byte[] padded = new byte[32];
        System.arraycopy(bytes, 0, padded, 0, bytes.length);
        return padded;
    }

    /** 受眾單元測試用的標準受眾常數列表。 */
    public static List<String> defaultAudience() {
        return List.of(AUDIENCE);
    }
}
