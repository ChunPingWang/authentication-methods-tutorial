package com.example.authtutorial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 應用程式進入點。
 *
 * <p>這個專案的目標：用「測試」來理解四種身分／授權概念
 * （SSO、OAuth 2.0、OIDC、SAML），並以六角形架構 (Hexagonal /
 * Ports &amp; Adapters) + DDD + SOLID 的方式組織程式碼。</p>
 *
 * <p>整個 {@code main} 啟動的 Spring 只是「最外圈」的轉接器與組態；
 * 真正的業務規則都放在不依賴框架的 domain 與 application 層，
 * 因此它們可以被快速、獨立地測試。</p>
 */
@SpringBootApplication
public class AuthenticationMethodsTutorialApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthenticationMethodsTutorialApplication.class, args);
    }
}
