package com.example.authtutorial.sso.domain.port.out;

import com.example.authtutorial.common.domain.Subject;
import com.example.authtutorial.sso.domain.model.Credentials;

import java.util.Optional;

/**
 * 被驅動端 (Driven / Outbound) 埠：抽象化「身分提供者 (IdP)」。
 *
 * <p>對應圖中右側的 Identity Providers（Google、Apple、Auth0…）。
 * application 層只知道「給憑證、回傳主體」這個契約，至於背後是 OpenLDAP、
 * Keycloak 還是記憶體假資料，由不同的「被驅動轉接器」決定。</p>
 */
public interface IdentityProviderPort {

    /**
     * 驗證憑證。
     *
     * @return 驗證成功則回傳主體；失敗回傳 {@link Optional#empty()}。
     */
    Optional<Subject> authenticate(Credentials credentials);
}
