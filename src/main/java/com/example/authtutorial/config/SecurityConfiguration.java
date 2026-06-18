package com.example.authtutorial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web 安全組態。
 *
 * <p><b>注意：</b>本教學的 REST 端點刻意「全部開放」，因為示範的重點是
 * 「在 domain 層親手實作」SSO/OAuth/OIDC/SAML 的驗證邏輯，而不是讓
 * Spring Security 自動幫我們擋下請求。正式專案請改為適當的授權規則
 * （例如 OAuth2 Resource Server 驗 JWT）。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
