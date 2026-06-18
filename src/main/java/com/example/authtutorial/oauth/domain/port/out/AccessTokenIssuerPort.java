package com.example.authtutorial.oauth.domain.port.out;

import com.example.authtutorial.common.domain.Subject;
import com.example.authtutorial.oauth.domain.model.AccessToken;
import com.example.authtutorial.oauth.domain.model.ClientId;
import com.example.authtutorial.oauth.domain.model.Scopes;

/**
 * 被驅動端埠：核發存取權杖。
 *
 * <p>把「如何產生並簽署權杖字串」這個技術細節抽象出去 ——
 * 可以是不透明 (opaque) 隨機字串，也可以是 JWT。domain 不在乎。</p>
 */
public interface AccessTokenIssuerPort {

    AccessToken issue(Subject resourceOwner, ClientId client, Scopes scopes);
}
