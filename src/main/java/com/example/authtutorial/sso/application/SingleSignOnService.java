package com.example.authtutorial.sso.application;

import com.example.authtutorial.common.domain.DomainException;
import com.example.authtutorial.common.domain.Subject;
import com.example.authtutorial.sso.domain.model.Credentials;
import com.example.authtutorial.sso.domain.model.SsoSession;
import com.example.authtutorial.sso.domain.port.in.SingleSignOnUseCase;
import com.example.authtutorial.sso.domain.port.out.IdentityProviderPort;
import com.example.authtutorial.sso.domain.port.out.SsoSessionRepository;

import java.time.Clock;
import java.time.Duration;

/**
 * 應用服務 (Application Service)：編排 (orchestrate) SSO 使用案例。
 *
 * <p>注意這個類別<b>不依賴任何框架</b>（沒有 {@code @Service}、沒有 Spring import）。
 * 它只透過建構子接收所需的「埠」，因此可以在純 JUnit 測試中用假的埠輕鬆驗證。
 * Spring 的組裝交給 {@code config} 套件處理，符合
 * <b>單一職責 (SRP)</b> 與<b>依賴反轉 (DIP)</b>。</p>
 */
public class SingleSignOnService implements SingleSignOnUseCase {

    private final IdentityProviderPort identityProvider;
    private final SsoSessionRepository sessionRepository;
    private final Clock clock;
    private final Duration sessionTtl;

    public SingleSignOnService(IdentityProviderPort identityProvider,
                               SsoSessionRepository sessionRepository,
                               Clock clock,
                               Duration sessionTtl) {
        this.identityProvider = identityProvider;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
        this.sessionTtl = sessionTtl;
    }

    @Override
    public SsoSession establishSession(EstablishSessionCommand command) {
        Credentials credentials = new Credentials(command.username(), command.password());

        Subject subject = identityProvider.authenticate(credentials)
                .orElseThrow(() -> new DomainException("認證失敗：使用者名稱或密碼錯誤"));

        SsoSession session = SsoSession.start(subject, clock.instant(), sessionTtl);
        sessionRepository.save(session);
        return session;
    }

    @Override
    public SsoSession accessApplication(AccessApplicationCommand command) {
        SsoSession session = sessionRepository.findById(command.sessionId())
                .orElseThrow(() -> new DomainException("找不到工作階段，請重新登入"));

        // 關鍵：這裡完全沒有再次驗證憑證 —— 這就是 SSO「免再次登入」的體現。
        session.accessApplication(command.applicationId(), clock.instant());
        sessionRepository.save(session);
        return session;
    }
}
