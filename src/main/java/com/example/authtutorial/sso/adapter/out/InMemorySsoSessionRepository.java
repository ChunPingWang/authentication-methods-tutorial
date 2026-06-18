package com.example.authtutorial.sso.adapter.out;

import com.example.authtutorial.sso.domain.model.SessionId;
import com.example.authtutorial.sso.domain.model.SsoSession;
import com.example.authtutorial.sso.domain.port.out.SsoSessionRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 被驅動轉接器：以記憶體實作工作階段儲存庫（教學用，重啟即清空）。
 *
 * <p>真實環境會換成 Redis / 資料庫轉接器，但 application 與 domain 完全不需改動 ——
 * 這正是六角形架構「可替換 (pluggable)」的價值。</p>
 */
@Repository
public class InMemorySsoSessionRepository implements SsoSessionRepository {

    private final Map<String, SsoSession> store = new ConcurrentHashMap<>();

    @Override
    public void save(SsoSession session) {
        store.put(session.id().value(), session);
    }

    @Override
    public Optional<SsoSession> findById(SessionId id) {
        return Optional.ofNullable(store.get(id.value()));
    }
}
