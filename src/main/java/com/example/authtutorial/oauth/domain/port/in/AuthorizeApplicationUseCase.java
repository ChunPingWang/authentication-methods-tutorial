package com.example.authtutorial.oauth.domain.port.in;

import com.example.authtutorial.oauth.domain.model.AccessToken;

import java.util.Set;

/**
 * 驅動端埠：OAuth 授權與資源存取的使用案例。
 */
public interface AuthorizeApplicationUseCase {

    /**
     * 授權流程：使用者同意後，核發存取權杖給 App。
     *
     * @throws com.example.authtutorial.common.domain.DomainException 未同意或 scope 超範圍時。
     */
    AccessToken authorize(AuthorizationCommand command);

    /**
     * 資源伺服器以權杖檢查「能否存取需要某 scope 的資源」。
     *
     * <p>注意：這裡判斷的是「權限」而非「身分」—— 體現 OAuth「控制能存取什麼、
     * 而非身分」的本質。</p>
     */
    ResourceAccessResult accessResource(String accessTokenValue, String requiredScope);

    /**
     * 授權指令。
     *
     * @param clientId        要求授權的 App。
     * @param resourceOwner   資源擁有者（使用者）。
     * @param requestedScopes App 要求的 scope。
     * @param approvedScopes  使用者實際核准的 scope（須為要求的子集）。
     * @param userConsented   使用者是否按下「同意」。
     */
    record AuthorizationCommand(
            String clientId,
            String resourceOwner,
            Set<String> requestedScopes,
            Set<String> approvedScopes,
            boolean userConsented) {
    }

    /** 資源存取結果。 */
    record ResourceAccessResult(boolean granted, String subject, Set<String> scopes, String reason) {
        public static ResourceAccessResult granted(String subject, Set<String> scopes) {
            return new ResourceAccessResult(true, subject, scopes, "OK");
        }

        public static ResourceAccessResult denied(String reason) {
            return new ResourceAccessResult(false, null, Set.of(), reason);
        }
    }
}
