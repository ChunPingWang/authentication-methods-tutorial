package com.example.authtutorial.sso.domain.port.in;

import com.example.authtutorial.sso.domain.model.SessionId;
import com.example.authtutorial.sso.domain.model.SsoSession;

/**
 * 驅動端 (Driving / Inbound) 埠：定義「外界能對 SSO 做什麼」。
 *
 * <p>這是六角形架構的左側入口。REST 控制器等「驅動轉接器」只依賴這個介面，
 * 而不直接認得 application 層的實作 —— 符合 SOLID 的<b>依賴反轉原則 (DIP)</b>。</p>
 */
public interface SingleSignOnUseCase {

    /**
     * 步驟 1～4：使用者登入一次，IdP 驗證憑證並建立工作階段。
     *
     * @throws com.example.authtutorial.common.domain.DomainException 若認證失敗。
     */
    SsoSession establishSession(EstablishSessionCommand command);

    /**
     * 步驟 5：以既有工作階段存取另一個 App，<b>無需重新登入</b>。
     *
     * @throws com.example.authtutorial.common.domain.DomainException 若 session 不存在或已過期。
     */
    SsoSession accessApplication(AccessApplicationCommand command);

    /** 登入指令（驅動轉接器把 HTTP 請求轉成此命令）。 */
    record EstablishSessionCommand(String username, String password) {
    }

    /** 存取 App 指令。 */
    record AccessApplicationCommand(SessionId sessionId, String applicationId) {
    }
}
