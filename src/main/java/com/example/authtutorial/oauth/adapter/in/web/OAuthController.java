package com.example.authtutorial.oauth.adapter.in.web;

import com.example.authtutorial.oauth.domain.model.AccessToken;
import com.example.authtutorial.oauth.domain.port.in.AuthorizeApplicationUseCase;
import com.example.authtutorial.oauth.domain.port.in.AuthorizeApplicationUseCase.ResourceAccessResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Set;

/**
 * 驅動轉接器：OAuth 2.0 的 HTTP 入口。
 */
@RestController
@RequestMapping("/api/oauth")
public class OAuthController {

    private final AuthorizeApplicationUseCase authorizeApplication;

    public OAuthController(AuthorizeApplicationUseCase authorizeApplication) {
        this.authorizeApplication = authorizeApplication;
    }

    /** 授權端點：使用者同意後核發存取權杖。 */
    @PostMapping("/authorize")
    public TokenResponse authorize(@RequestBody AuthorizeRequest request) {
        AccessToken token = authorizeApplication.authorize(
                new AuthorizeApplicationUseCase.AuthorizationCommand(
                        request.clientId(),
                        request.resourceOwner(),
                        request.requestedScopes(),
                        request.approvedScopes(),
                        request.userConsented()));
        return new TokenResponse(token.value(), token.scopes().asStringSet(), token.expiresAt());
    }

    /** 資源端點：以 Bearer 權杖檢查能否存取需要某 scope 的資源。 */
    @PostMapping("/resource")
    public ResourceAccessResult resource(@RequestParam("token") String token,
                                         @RequestParam("requiredScope") String requiredScope) {
        return authorizeApplication.accessResource(token, requiredScope);
    }

    public record AuthorizeRequest(
            @NotBlank String clientId,
            @NotBlank String resourceOwner,
            @NotEmpty Set<String> requestedScopes,
            @NotEmpty Set<String> approvedScopes,
            boolean userConsented) {
    }

    public record TokenResponse(String accessToken, Set<String> scopes, Instant expiresAt) {
    }
}
