#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────
# 環境設定（供 Claude Code on the web 的 SessionStart hook 與本機使用）
#
# 本專案需要 JDK 23。此腳本會：
#   1. 若尚未安裝 JDK 23，下載 Temurin 23 到 /opt/jdk23（best-effort）。
#   2. 把環境的 egress proxy CA 匯入 JDK 23 truststore，讓 Maven 能走 TLS。
#   3. 印出 JAVA_HOME 提示。
#
# 全程「盡力而為」，任何步驟失敗都不會讓整個 session 失敗。
# ─────────────────────────────────────────────────────────────────────────
set -uo pipefail

JDK_DIR="/opt/jdk23"
JDK_URL="https://github.com/adoptium/temurin23-binaries/releases/download/jdk-23.0.2%2B7/OpenJDK23U-jdk_x64_linux_hotspot_23.0.2_7.tar.gz"

install_jdk23() {
  if [ -x "$JDK_DIR/bin/java" ]; then
    echo "[setup] JDK 23 已存在於 $JDK_DIR"
    return 0
  fi
  echo "[setup] 下載 JDK 23 ..."
  mkdir -p "$JDK_DIR" || return 0
  curl -fsSL -o /tmp/jdk23.tar.gz "$JDK_URL" || { echo "[setup] 下載 JDK 失敗（略過）"; return 0; }
  tar xzf /tmp/jdk23.tar.gz -C "$JDK_DIR" --strip-components=1 || return 0
  echo "[setup] JDK 23 安裝完成"
}

import_proxy_ca() {
  local ks="$JDK_DIR/lib/security/cacerts"
  [ -f "$ks" ] || return 0
  for crt in /usr/local/share/ca-certificates/*.crt; do
    [ -f "$crt" ] || continue
    local alias
    alias="$(basename "$crt" .crt)"
    "$JDK_DIR/bin/keytool" -importcert -noprompt -trustcacerts \
      -alias "$alias" -file "$crt" -keystore "$ks" -storepass changeit >/dev/null 2>&1 || true
  done
  echo "[setup] 已嘗試匯入 proxy CA 憑證"
}

install_jdk23
import_proxy_ca

echo "[setup] 完成。請使用： export JAVA_HOME=$JDK_DIR && export PATH=\$JAVA_HOME/bin:\$PATH"
echo "[setup] 然後執行： ./mvnw test"
exit 0
