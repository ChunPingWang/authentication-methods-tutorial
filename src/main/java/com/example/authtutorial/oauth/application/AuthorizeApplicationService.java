package com.example.authtutorial.oauth.application;

import com.example.authtutorial.common.domain.Subject;
import com.example.authtutorial.oauth.domain.model.AccessToken;
import com.example.authtutorial.oauth.domain.model.AuthorizationGrant;
import com.example.authtutorial.oauth.domain.model.ClientId;
import com.example.authtutorial.oauth.domain.model.Scope;
import com.example.authtutorial.oauth.domain.model.Scopes;
import com.example.authtutorial.oauth.domain.port.in.AuthorizeApplicationUseCase;
import com.example.authtutorial.oauth.domain.port.out.AccessTokenIntrospectionPort;
import com.example.authtutorial.oauth.domain.port.out.AccessTokenIssuerPort;

import java.time.Clock;

/**
 * 應用服務：編排 OAuth 授權與資源存取流程（不依賴框架）。
 */
public class AuthorizeApplicationService implements AuthorizeApplicationUseCase {

    private final AccessTokenIssuerPort tokenIssuer;
    private final AccessTokenIntrospectionPort introspection;
    private final Clock clock;

    public AuthorizeApplicationService(AccessTokenIssuerPort tokenIssuer,
                                       AccessTokenIntrospectionPort introspection,
                                       Clock clock) {
        this.tokenIssuer = tokenIssuer;
        this.introspection = introspection;
        this.clock = clock;
    }

    @Override
    public AccessToken authorize(AuthorizationCommand command) {
        ClientId client = ClientId.of(command.clientId());
        Subject owner = Subject.of(command.resourceOwner());
        Scopes requested = Scopes.fromStrings(command.requestedScopes());
        Scopes approved = Scopes.fromStrings(command.approvedScopes());

        // 由聚合根守護「同意」與「scope 子集」兩條規則。
        AuthorizationGrant grant = AuthorizationGrant.request(client, owner, requested);
        if (command.userConsented()) {
            grant.giveConsent();
        }
        Scopes effectiveScopes = grant.approve(approved);

        // 規則通過後，才委派給「被驅動端」核發實際權杖。
        return tokenIssuer.issue(owner, client, effectiveScopes);
    }

    @Override
    public ResourceAccessResult accessResource(String accessTokenValue, String requiredScope) {
        return introspection.introspect(accessTokenValue)
                .map(token -> {
                    if (token.authorizes(Scope.of(requiredScope), clock.instant())) {
                        return ResourceAccessResult.granted(
                                token.resourceOwner().value(), token.scopes().asStringSet());
                    }
                    if (token.isExpired(clock.instant())) {
                        return ResourceAccessResult.denied("權杖已過期");
                    }
                    return ResourceAccessResult.denied("權杖缺少所需的 scope: " + requiredScope);
                })
                .orElseGet(() -> ResourceAccessResult.denied("無效的權杖"));
    }
}
