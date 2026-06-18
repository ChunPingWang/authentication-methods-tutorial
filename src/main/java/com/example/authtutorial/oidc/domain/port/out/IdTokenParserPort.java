package com.example.authtutorial.oidc.domain.port.out;

import com.example.authtutorial.oidc.domain.model.IdTokenClaims;

/**
 * 被驅動端埠：解析 JWT 並<b>驗證其簽章</b>，回傳其中的 claims。
 *
 * <p>「如何驗簽章」是技術細節（HMAC 共享金鑰、或向 IdP 的 JWKS 取公鑰），
 * domain 不在乎，只要拿到可信任的 claims 即可。</p>
 */
public interface IdTokenParserPort {

    /**
     * @throws com.example.authtutorial.common.domain.DomainException 簽章無效或格式錯誤。
     */
    IdTokenClaims parseAndVerifySignature(String rawIdToken);
}
