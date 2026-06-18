package com.example.authtutorial.oidc.domain.port.in;

import com.example.authtutorial.oidc.domain.model.VerifiedIdentity;

/**
 * 驅動端埠：以 ID Token 驗證並取得使用者身分。
 */
public interface VerifyIdentityUseCase {

    /**
     * 驗證 ID Token：先驗簽章（被驅動端），再驗 claims（domain），最後回傳身分。
     *
     * @throws com.example.authtutorial.common.domain.DomainException 簽章或 claims 驗證失敗。
     */
    VerifiedIdentity verify(VerifyIdTokenCommand command);

    /**
     * @param rawIdToken       原始 JWT 字串。
     * @param expectedAudience 預期受眾（本 App 的 client id）。
     * @param expectedNonce    預期 nonce（可為 null）。
     */
    record VerifyIdTokenCommand(String rawIdToken, String expectedAudience, String expectedNonce) {
    }
}
