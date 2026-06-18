# ── 建置階段：用 JDK 23 + Maven 打包 ──────────────────────────────────────
FROM eclipse-temurin:23-jdk AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
# 先離線下載相依（利用快取層），再打包（測試在 CI 已跑過，這裡略過）
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -DskipTests package || mvn -B -q -DskipTests package

# ── 執行階段：精簡的 JRE 23 ───────────────────────────────────────────────
FROM eclipse-temurin:23-jre
WORKDIR /app
COPY --from=build /workspace/target/authentication-methods-tutorial-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
