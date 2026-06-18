package com.example.authtutorial.sso.adapter.in.web;

import com.example.authtutorial.sso.domain.model.SessionId;
import com.example.authtutorial.sso.domain.model.SsoSession;
import com.example.authtutorial.sso.domain.port.in.SingleSignOnUseCase;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Set;

/**
 * 驅動轉接器：把 HTTP 請求翻譯成對 {@link SingleSignOnUseCase} 的呼叫。
 *
 * <p>控制器只依賴「驅動端埠」介面，不認得 application 的實作類別。</p>
 */
@RestController
@RequestMapping("/api/sso")
public class SsoController {

    private final SingleSignOnUseCase singleSignOn;

    public SsoController(SingleSignOnUseCase singleSignOn) {
        this.singleSignOn = singleSignOn;
    }

    /** 登入一次，取得工作階段。 */
    @PostMapping("/login")
    public SessionResponse login(@RequestBody LoginRequest request) {
        SsoSession session = singleSignOn.establishSession(
                new SingleSignOnUseCase.EstablishSessionCommand(request.username(), request.password()));
        return SessionResponse.from(session);
    }

    /** 用既有工作階段存取某個 App（免再次登入）。 */
    @PostMapping("/sessions/{sessionId}/access")
    public SessionResponse access(@PathVariable String sessionId,
                                  @RequestBody AccessRequest request) {
        SsoSession session = singleSignOn.accessApplication(
                new SingleSignOnUseCase.AccessApplicationCommand(
                        SessionId.of(sessionId), request.applicationId()));
        return SessionResponse.from(session);
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record AccessRequest(@NotBlank String applicationId) {
    }

    public record SessionResponse(String sessionId, String subject,
                                  Instant expiresAt, Set<String> accessedApplications) {
        static SessionResponse from(SsoSession session) {
            return new SessionResponse(
                    session.id().value(),
                    session.subject().value(),
                    session.expiresAt(),
                    session.accessedApplications());
        }
    }
}
