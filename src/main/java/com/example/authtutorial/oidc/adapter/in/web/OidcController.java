package com.example.authtutorial.oidc.adapter.in.web;

import com.example.authtutorial.oidc.domain.model.VerifiedIdentity;
import com.example.authtutorial.oidc.domain.port.in.VerifyIdentityUseCase;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 驅動轉接器：OIDC 的 HTTP 入口。App 把收到的 ID Token 拿來驗證身分。
 */
@RestController
@RequestMapping("/api/oidc")
public class OidcController {

    private final VerifyIdentityUseCase verifyIdentity;

    public OidcController(VerifyIdentityUseCase verifyIdentity) {
        this.verifyIdentity = verifyIdentity;
    }

    @PostMapping("/verify")
    public IdentityResponse verify(@RequestBody VerifyRequest request) {
        VerifiedIdentity identity = verifyIdentity.verify(
                new VerifyIdentityUseCase.VerifyIdTokenCommand(
                        request.idToken(), request.audience(), request.nonce()));
        return new IdentityResponse(
                identity.subject().value(), identity.email(), identity.issuer());
    }

    public record VerifyRequest(@NotBlank String idToken, @NotBlank String audience, String nonce) {
    }

    public record IdentityResponse(String subject, String email, String issuer) {
    }
}
