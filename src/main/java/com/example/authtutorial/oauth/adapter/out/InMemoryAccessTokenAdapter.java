package com.example.authtutorial.oauth.adapter.out;

import com.example.authtutorial.common.domain.Subject;
import com.example.authtutorial.oauth.domain.model.AccessToken;
import com.example.authtutorial.oauth.domain.model.ClientId;
import com.example.authtutorial.oauth.domain.model.Scopes;
import com.example.authtutorial.oauth.domain.port.out.AccessTokenIntrospectionPort;
import com.example.authtutorial.oauth.domain.port.out.AccessTokenIssuerPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 被驅動轉接器：同時實作「核發」與「內省」兩個埠。
 *
 * <p>核發不透明 (opaque) 隨機字串權杖，並記在記憶體中供內省查詢。
 * 真實授權伺服器（如 Keycloak）會以 JWT 或自家儲存實作相同契約。</p>
 */
@Component
public class InMemoryAccessTokenAdapter implements AccessTokenIssuerPort, AccessTokenIntrospectionPort {

    private static final Duration TOKEN_TTL = Duration.ofHours(1);

    private final Clock clock;
    private final Map<String, AccessToken> issuedTokens = new ConcurrentHashMap<>();

    public InMemoryAccessTokenAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public AccessToken issue(Subject resourceOwner, ClientId client, Scopes scopes) {
        var now = clock.instant();
        AccessToken token = new AccessToken(
                "at_" + UUID.randomUUID().toString().replace("-", ""),
                resourceOwner,
                client,
                scopes,
                now,
                now.plus(TOKEN_TTL));
        issuedTokens.put(token.value(), token);
        return token;
    }

    @Override
    public Optional<AccessToken> introspect(String accessTokenValue) {
        return Optional.ofNullable(issuedTokens.get(accessTokenValue));
    }
}
