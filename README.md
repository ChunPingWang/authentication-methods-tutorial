# 認證方法教學：SSO vs OAuth vs OIDC vs SAML

> 以**測試驅動**的方式，搭配 **Hexagonal Architecture（六角形 / Ports & Adapters）+ DDD + SOLID**，
> 用 **JDK 23 + Spring Boot 4** 親手實作並驗證四種身分／授權概念。

這個專案的靈感來自下面兩張圖（放在 [`docs/images`](docs/images/)）：

| 概念說明 | 流程圖 |
| --- | --- |
| ![概念](docs/images/concepts-text.jpeg) | ![流程](docs/images/concepts-flows.jpeg) |

---

## 目錄
1. [四個概念，一句話分清楚](#四個概念一句話分清楚)
2. [為什麼從「測試」出發？](#為什麼從測試出發)
3. [架構：六角形 + DDD + SOLID](#架構六角形--ddd--solid)
4. [專案結構](#專案結構)
5. [快速開始](#快速開始)
6. [四種流程逐一拆解（對照測試）](#四種流程逐一拆解對照測試)
7. [進階：用真實 Keycloak 做 OIDC 整合測試](#進階用真實-keycloak-做-oidc-整合測試)
8. [Docker Compose / Kubernetes / OpenLDAP](#docker-compose--kubernetes--openldap)
9. [初學者常見問題](#初學者常見問題)

---

## 四個概念，一句話分清楚

| 概念 | 它是什麼 | 回答的問題 | 本專案對應 |
| --- | --- | --- | --- |
| **SSO** | 一種**使用者體驗**，不是協定。登入一次就能存取多個 App。 | —（底層靠 SAML/OIDC） | `sso` 套件 |
| **OAuth 2.0** | **授權 (Authorization)** 框架。讓 App 在不拿到密碼的情況下存取資源。 | 「這個 App **能存取什麼**？」 | `oauth` 套件 |
| **OIDC** | 架在 OAuth 2.0 之上的**身分驗證 (Authentication)** 層，發 ID Token（JWT）。 | 「**你是誰**？」 | `oidc` 套件 |
| **SAML** | 較早期、以 **XML** 為基礎的企業級 SSO 認證協定。 | 「**你是誰**？」（企業/傳統系統） | `saml` 套件 |

> 一句話：**OAuth 管「能做什麼」；OIDC / SAML 管「你是誰」；SSO 是「只登一次」的體驗。**

---

## 為什麼從「測試」出發？

對初學者而言，安全協定最難的是「看不到、摸不到」。本專案把每個概念的**核心規則**寫成
**會執行、會綠燈/紅燈**的測試，例如：

- SSO：「過期的工作階段不能再存取 App」→ `SsoSessionTest`
- OAuth：「未經同意不得核發 token」「核准的 scope 不能超出要求」→ `AuthorizationGrantTest`
- OIDC：「ID Token 過期 / 受眾不符 / nonce 不符要被擋下」→ `IdTokenClaimsTest`、`VerifyIdentityServiceTest`
- SAML：「斷言時間區間與受眾驗證」「竄改要被偵測」→ `SamlAssertionTest`、`DemoSamlCodecTest`

測試金字塔（本專案實際採用）：

```
        /\        整合測試 (1)    KeycloakOidcVerificationIT  ← 需要 Docker，預設不跑
       /  \       端對端 Web (1)  AuthenticationFlowsWebTest  ← 完整 Spring + MockMvc
      /----\      架構測試 (1)    HexagonalArchitectureTest   ← ArchUnit 守護相依規則
     /      \     單元/應用 (10)  domain 與 application 的純 Java 測試（毫秒級）
    /--------\
```

執行 `mvn test` 會跑 **53 個測試**，全部不需要 Docker、不需要外部服務。

---

## 架構：六角形 + DDD + SOLID

```
              驅動端 (Driving)                          被驅動端 (Driven)
        ┌──────────────────────┐                  ┌──────────────────────────┐
   HTTP │  REST Controller     │                  │  InMemory / Keycloak /    │ DB / IdP
  ─────▶│  (adapter.in.web)    │                  │  HMAC 解碼器 (adapter.out)│ ◀─────
        └──────────┬───────────┘                  └────────────▲─────────────┘
                   │ 依賴                                       │ 實作
            ┌──────▼───────┐  in port        out port  ┌───────┴───────┐
            │  UseCase 介面 │◀───────┐        ┌────────▶│  Repository / │
            └──────┬───────┘        │        │         │  Gateway 介面 │
                   │ 實作       ┌────┴────────┴───┐     └───────────────┘
            ┌──────▼───────────│  Application     │
            │  純 Java 應用服務 │  Service          │  ← 不依賴任何框架
            └──────┬───────────└──────────────────┘
                   │ 使用
            ┌──────▼─────────────────────────────┐
            │  Domain：聚合根 / 值物件 / 業務規則   │  ← 專案的心臟，零框架依賴
            └────────────────────────────────────┘
```

落實的原則：

- **依賴反轉 (DIP)**：`domain` 定義 `port`（介面），`adapter` 實作它；相依方向永遠**由外向內**。
- **單一職責 (SRP)**：domain 管規則、application 管編排、adapter 管 I/O 與框架。
- **可替換 (OCP/LSP)**：把記憶體版的 IdP 換成 OpenLDAP / Keycloak，domain 與 application **一行都不用改**。
- **介面隔離 (ISP)**：每個 port 都小而專一（如 `AccessTokenIssuerPort`、`AccessTokenIntrospectionPort` 分開）。
- **架構即測試**：`HexagonalArchitectureTest`（ArchUnit）會在 CI 擋下任何「domain 依賴 Spring」之類的違規。

> 組合根 (Composition Root) 只有一個：[`config/BeanConfiguration`](src/main/java/com/example/authtutorial/config/BeanConfiguration.java)。
> 它是**唯一**把 domain 埠和具體 adapter 接起來的地方。

---

## 專案結構

每個概念都是一個 **bounded context**，內部都遵循相同的六角形分層：

```
src/main/java/com/example/authtutorial/
├── common/domain/              共享核心：Subject、DomainException
├── sso/                        ── SSO：登入一次、存取多 App
│   ├── domain/model/           SsoSession（聚合根）、SessionId、Credentials
│   ├── domain/port/in/         SingleSignOnUseCase（驅動端埠）
│   ├── domain/port/out/        IdentityProviderPort、SsoSessionRepository（被驅動端埠）
│   ├── application/            SingleSignOnService（純 Java 編排）
│   └── adapter/in|out/         REST 控制器、記憶體 IdP / 儲存庫
├── oauth/                      ── OAuth 2.0：授權、scope、同意、access token
├── oidc/                       ── OIDC：ID Token（JWT）驗證 → 身分
├── saml/                       ── SAML：簽章斷言驗證 → 身分
└── config/                     組合根、安全組態
```

對應的測試 `src/test/java/...` 與正式碼一一對映。

---

## 快速開始

### 需求
- **JDK 23**（本專案以 JDK 23 編譯；Spring Boot 4 需要 JDK 17+）
- 連線到 Maven Central（首次建置會下載相依）

### 跑測試（不需要 Docker）
```bash
./mvnw test
```
你會看到 53 個測試全部通過 —— 這就是「驗證所有功能」。

### 啟動應用程式
```bash
./mvnw spring-boot:run
```
然後用 [`docs/api-examples.http`](docs/api-examples.http) 逐一打 API（或用 curl，見下節）。

---

## 四種流程逐一拆解（對照測試）

### 1) SSO — 「登入一次，存取多個 App」

對應流程圖：使用者試圖存取 App A → 被導向 IdP → 登入一次 → IdP 建立 session → 之後存取 App B **免再登入**。

```bash
# 登入一次
curl -s localhost:8080/api/sso/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"wonderland"}'
# → { "sessionId": "...", "subject": "alice", ... }

# 用 sessionId 存取多個 App（不再送密碼）
curl -s localhost:8080/api/sso/sessions/<SESSION_ID>/access \
  -H 'Content-Type: application/json' -d '{"applicationId":"gmail"}'
```

核心規則在 [`SsoSession`](src/main/java/com/example/authtutorial/sso/domain/model/SsoSession.java)：
工作階段一旦過期，`accessApplication` 就會丟出 `DomainException`。對應測試
[`SsoSessionTest`](src/test/java/com/example/authtutorial/sso/domain/model/SsoSessionTest.java)。

### 2) OAuth 2.0 — 「授權，不是身分」

對應流程圖：App A 需要存取 App B 的資料 → 導向授權伺服器 → 使用者登入並**同意** →
取得 access token → 用 token 呼叫 App B 的 API。

```bash
# 使用者同意後核發 token（注意 approvedScopes ⊆ requestedScopes）
curl -s localhost:8080/api/oauth/authorize -H 'Content-Type: application/json' -d '{
  "clientId":"app-A","resourceOwner":"alice",
  "requestedScopes":["contacts.read","contacts.write"],
  "approvedScopes":["contacts.read"],"userConsented":true }'

# 用 token 存取需要某 scope 的資源
curl -s -X POST "localhost:8080/api/oauth/resource?token=<TOKEN>&requiredScope=contacts.read"
```

兩條關鍵業務規則由聚合根 [`AuthorizationGrant`](src/main/java/com/example/authtutorial/oauth/domain/model/AuthorizationGrant.java) 守護：
**未同意不發 token**、**核准的 scope 不可超出要求**。對應測試
[`AuthorizationGrantTest`](src/test/java/com/example/authtutorial/oauth/domain/model/AuthorizationGrantTest.java)、
[`AuthorizeApplicationServiceTest`](src/test/java/com/example/authtutorial/oauth/application/AuthorizeApplicationServiceTest.java)。

### 3) OIDC — 「你是誰」（ID Token / JWT）

對應流程圖：使用者認證 → IdP 發 ID Token → App **驗證 ID token 並萃取身分**。

驗證分兩段，正好對應六角形的兩側：
1. **被驅動端**驗 JWT **簽章**（[`JwtDecoderIdTokenParser`](src/main/java/com/example/authtutorial/oidc/adapter/out/JwtDecoderIdTokenParser.java)，底層 Nimbus）。
2. **domain** 驗業務 **claims**（過期 / 發行者 / 受眾 / nonce），見
   [`IdTokenClaims.validate`](src/main/java/com/example/authtutorial/oidc/domain/model/IdTokenClaims.java)。

對應測試 [`VerifyIdentityServiceTest`](src/test/java/com/example/authtutorial/oidc/application/VerifyIdentityServiceTest.java)
走的是**真實的 HS256 簽章驗證**，但用共享密鑰，所以**完全離線**就能跑。
想用真正的 IdP？見下一節的 Keycloak 整合測試。

### 4) SAML — 「你是誰」（XML 斷言，企業常用）

對應流程圖：使用者認證 → IdP 建立**簽章後的 SAML 斷言** → App **驗證簽章並萃取身分**。

同樣是「先驗簽章、再驗條件 (Conditions)」：時間區間、發行者、受眾，見
[`SamlAssertion.validate`](src/main/java/com/example/authtutorial/saml/domain/model/SamlAssertion.java)。

> **教學簡化**：真正的 SAML 用 XML 與 XML-DSig（通常以 OpenSAML / Spring Security SAML 實作）。
> 為了讓初學者聚焦在「驗簽章 → 驗條件」的**架構邊界**而非 XML 細節，本專案的
> [`DemoSamlCodec`](src/main/java/com/example/authtutorial/saml/adapter/out/DemoSamlCodec.java)
> 以 `base64(JSON).HMAC` 模擬「已簽章的斷言」。概念完全相同。只要替換這個 adapter，
> domain 與 application 不需更動。

---

## 進階：用真實 Keycloak 做 OIDC 整合測試

[`KeycloakOidcVerificationIT`](src/test/java/com/example/authtutorial/oidc/integration/KeycloakOidcVerificationIT.java)
用 **Testcontainers** 啟動真正的 Keycloak（匯入 [`tutorial-realm.json`](src/test/resources/keycloak/tutorial-realm.json)），
向它換取**真實的 ID Token**，再用 Keycloak 的 JWKS 公鑰跑我們的 `VerifyIdentityService`。

```bash
# 需要可用的 Docker，且能拉取 quay.io/keycloak/keycloak 映像檔
./mvnw verify -Pintegration
```

> 預設的 `mvn test` 會**排除**標記為 `@Tag("integration")` 的測試（見 `pom.xml` 的 surefire/failsafe 設定），
> 因此在沒有 Docker 的環境（如部分 CI / 雲端沙箱）依然能綠燈通過。

這示範了六角形架構的威力：**同一個 domain / application，換上不同的被驅動端 adapter
（HMAC 離線解碼器 ↔ Keycloak JWKS 解碼器），就能在「快速離線測試」與「真實 IdP 驗證」之間自由切換。**

---

## Docker Compose / Kubernetes / OpenLDAP

### 本機手動把玩（選配）
```bash
docker compose up -d
# Keycloak  http://localhost:8081 (admin/admin)，已匯入 tutorial realm
# OpenLDAP  ldap://localhost:389（企業目錄，可作為 SSO 後端）
# phpLDAPadmin http://localhost:8082
```
`docker-compose.yml` 裡的 **OpenLDAP** 示範了「企業目錄」這個常見的 SSO 後端 ——
你可以新增一個 `LdapIdentityProviderAdapter implements IdentityProviderPort`，
把 SSO 的身分來源從記憶體換成 LDAP，而完全不動 domain。

### Kubernetes
```bash
docker build -t auth-tutorial:1.0.0 .
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/keycloak.yaml
kubectl apply -f k8s/app.yaml
# 應用程式的 OIDC 信任發行者已指向叢集內的 Keycloak（見 k8s/app.yaml）
```

---

## 初學者常見問題

**Q：OAuth 和 OIDC 到底差在哪？**
A：OAuth 給你的是 **access token**（代表「權限」），拿去呼叫 API；OIDC 額外給你 **ID Token**
（代表「身分」），拿來知道使用者是誰。本專案 `oauth` 與 `oidc` 兩個套件刻意分開，讓你看清差異。

**Q：為什麼 domain 不能 import Spring？**
A：把業務規則和框架解耦，才能（1）用純 JUnit 毫秒級測試、（2）將來換框架/換 IdP 不傷核心。
`HexagonalArchitectureTest` 會自動幫你守住這條線。

**Q：為什麼用 `record` 和 `Clock`？**
A：`record` 讓值物件天生不可變、好比較；注入 `Clock` 讓「過期」這類時間規則能用**固定時間**穩定測試。

**Q：這些密鑰可以上正式環境嗎？**
A：**不行**。`application.yml` 裡的 HMAC / SAML 密鑰只供教學。正式環境請改用 IdP 的非對稱金鑰（RS256 + JWKS）。

---

### 授權
本教學程式碼僅供學習使用。
