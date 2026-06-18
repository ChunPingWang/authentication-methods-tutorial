package com.example.authtutorial.oauth.domain.port.out;

import com.example.authtutorial.oauth.domain.model.AccessToken;

import java.util.Optional;

/**
 * 被驅動端埠：權杖內省 (introspection)。
 *
 * <p>資源伺服器拿到不透明權杖字串時，需要向授權伺服器查詢其內容
 * （對應 OAuth 的 token introspection）。這裡抽象成一個查詢埠。</p>
 */
public interface AccessTokenIntrospectionPort {

    Optional<AccessToken> introspect(String accessTokenValue);
}
