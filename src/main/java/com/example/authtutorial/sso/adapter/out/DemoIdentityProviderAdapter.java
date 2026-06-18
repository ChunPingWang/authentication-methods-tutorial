package com.example.authtutorial.sso.adapter.out;

import com.example.authtutorial.common.domain.Subject;
import com.example.authtutorial.sso.domain.model.Credentials;
import com.example.authtutorial.sso.domain.port.out.IdentityProviderPort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 被驅動轉接器：示範用的身分提供者，內建幾組固定帳密。
 *
 * <p>正式環境可換成 OpenLDAP 或 Keycloak 轉接器（見 README 的進階章節），
 * 只要同樣實作 {@link IdentityProviderPort} 即可，不影響其他層。</p>
 */
@Component
public class DemoIdentityProviderAdapter implements IdentityProviderPort {

    /** 示範帳號：alice / wonderland、bob / builder。 */
    private static final Map<String, String> USERS = Map.of(
            "alice", "wonderland",
            "bob", "builder"
    );

    @Override
    public Optional<Subject> authenticate(Credentials credentials) {
        String expected = USERS.get(credentials.username());
        if (expected != null && expected.equals(credentials.password())) {
            return Optional.of(Subject.of(credentials.username()));
        }
        return Optional.empty();
    }
}
