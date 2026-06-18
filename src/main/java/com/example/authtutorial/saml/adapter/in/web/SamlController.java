package com.example.authtutorial.saml.adapter.in.web;

import com.example.authtutorial.saml.domain.model.AssertedIdentity;
import com.example.authtutorial.saml.domain.port.in.ValidateAssertionUseCase;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 驅動轉接器：SAML 的 HTTP 入口（SP 端接收並驗證斷言）。
 */
@RestController
@RequestMapping("/api/saml")
public class SamlController {

    private final ValidateAssertionUseCase validateAssertion;

    public SamlController(ValidateAssertionUseCase validateAssertion) {
        this.validateAssertion = validateAssertion;
    }

    @PostMapping("/validate")
    public IdentityResponse validate(@RequestBody ValidateRequest request) {
        AssertedIdentity identity = validateAssertion.validate(
                new ValidateAssertionUseCase.ValidateAssertionCommand(
                        request.assertion(), request.audience()));
        return new IdentityResponse(
                identity.subject().value(), identity.issuer(), identity.attributes());
    }

    public record ValidateRequest(@NotBlank String assertion, @NotBlank String audience) {
    }

    public record IdentityResponse(String subject, String issuer, Map<String, String> attributes) {
    }
}
