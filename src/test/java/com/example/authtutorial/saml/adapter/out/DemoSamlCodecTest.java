package com.example.authtutorial.saml.adapter.out;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.saml.domain.model.SamlAssertion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DemoSamlCodec} 測試：示範「簽章 → 驗證」的往返，以及竄改偵測。
 */
@DisplayName("DemoSamlCodec（簽章/驗證）")
class DemoSamlCodecTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private final DemoSamlCodec codec = new DemoSamlCodec("a-shared-secret");

    private SamlAssertion sampleAssertion() {
        return new SamlAssertion("https://idp.tutorial.local", "alice", "https://sp.tutorial.local",
                NOW.minusSeconds(60), NOW.plusSeconds(300), Map.of("role", "admin"));
    }

    @Test
    @DisplayName("簽發後再驗證 → 取回相同內容 (round-trip)")
    void signThenVerifyRoundTrip() {
        String signed = codec.sign(sampleAssertion());

        SamlAssertion parsed = codec.verifyAndParse(signed);

        assertThat(parsed.subject()).isEqualTo("alice");
        assertThat(parsed.attributes()).containsEntry("role", "admin");
    }

    @Test
    @DisplayName("內容被竄改 → 簽章驗證失敗")
    void detectsTampering() {
        String signed = codec.sign(sampleAssertion());
        // 竄改 payload 部分。
        String tampered = "ZXZpbA" + signed.substring(6);

        assertThatThrownBy(() -> codec.verifyAndParse(tampered))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("簽章驗證失敗");
    }

    @Test
    @DisplayName("以不同密鑰驗證 → 失敗")
    void rejectsWrongKey() {
        String signed = codec.sign(sampleAssertion());
        DemoSamlCodec attacker = new DemoSamlCodec("different-secret");

        assertThatThrownBy(() -> attacker.verifyAndParse(signed))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("格式錯誤 → 被拒絕")
    void rejectsMalformed() {
        assertThatThrownBy(() -> codec.verifyAndParse("not-a-valid-assertion"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("格式錯誤");
    }
}
