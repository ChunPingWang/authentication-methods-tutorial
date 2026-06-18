package com.example.authtutorial.saml.adapter.out;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.saml.domain.model.SamlAssertion;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * 簡化版 SAML 斷言「編解碼 + 簽章」工具。
 *
 * <p><b>教學簡化說明：</b>真正的 SAML 使用 XML 與 XML 數位簽章 (XML-DSig)，
 * 通常以 OpenSAML 或 Spring Security SAML 實作。為了讓初學者聚焦在
 * 「先驗簽章、再驗條件」的<b>架構邊界</b>而非 XML 細節，這裡用
 * {@code base64(JSON).base64(HMAC-SHA256)} 的格式來模擬「已簽章的斷言」。
 * 概念完全相同：拿到資料 → 驗證簽章 → 才信任內容。</p>
 */
public final class DemoSamlCodec {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final String secret;

    public DemoSamlCodec(String secret) {
        this.secret = secret;
    }

    /** 由 IdP 端用來「簽發」一個斷言（測試與示範用）。 */
    public String sign(SamlAssertion assertion) {
        try {
            Map<String, Object> payload = Map.of(
                    "issuer", assertion.issuer(),
                    "subject", assertion.subject(),
                    "audience", assertion.audience(),
                    "notBefore", assertion.notBefore().getEpochSecond(),
                    "notOnOrAfter", assertion.notOnOrAfter().getEpochSecond(),
                    "attributes", assertion.attributes());
            byte[] json = MAPPER.writeValueAsBytes(payload);
            String payloadB64 = ENCODER.encodeToString(json);
            String sig = ENCODER.encodeToString(hmac(payloadB64));
            return payloadB64 + "." + sig;
        } catch (Exception e) {
            throw new DomainException("無法簽發斷言: " + e.getMessage());
        }
    }

    /** 由 SP 端用來「驗證簽章並解析」斷言。 */
    @SuppressWarnings("unchecked")
    public SamlAssertion verifyAndParse(String raw) {
        if (raw == null || !raw.contains(".")) {
            throw new DomainException("SAML 斷言格式錯誤");
        }
        String[] parts = raw.split("\\.", 2);
        String payloadB64 = parts[0];
        byte[] providedSig = decodeSig(parts[1]);

        // 以固定時間比較避免時序攻擊。
        if (!MessageDigest.isEqual(hmac(payloadB64), providedSig)) {
            throw new DomainException("SAML 斷言簽章驗證失敗");
        }

        try {
            Map<String, Object> payload = MAPPER.readValue(DECODER.decode(payloadB64), Map.class);
            return new SamlAssertion(
                    (String) payload.get("issuer"),
                    (String) payload.get("subject"),
                    (String) payload.get("audience"),
                    Instant.ofEpochSecond(((Number) payload.get("notBefore")).longValue()),
                    Instant.ofEpochSecond(((Number) payload.get("notOnOrAfter")).longValue()),
                    (Map<String, String>) payload.getOrDefault("attributes", Map.of()));
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException("無法解析 SAML 斷言: " + e.getMessage());
        }
    }

    private byte[] decodeSig(String sig) {
        try {
            return DECODER.decode(sig);
        } catch (IllegalArgumentException e) {
            throw new DomainException("SAML 斷言簽章編碼錯誤");
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new DomainException("HMAC 計算失敗: " + e.getMessage());
        }
    }
}
