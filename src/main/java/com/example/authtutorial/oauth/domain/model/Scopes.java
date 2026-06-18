package com.example.authtutorial.oauth.domain.model;

import com.example.authtutorial.common.domain.DomainException;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 值物件：一組 scope 的集合，封裝「子集 (subset)」這個重要的業務規則。
 *
 * <p>授權時，使用者實際核准的 scope 必須是 App「要求的 scope」的子集 ——
 * App 不能拿到比使用者同意更多的權限。這個規則由本類別保證。</p>
 */
public final class Scopes {

    private final Set<Scope> values;

    private Scopes(Set<Scope> values) {
        this.values = values;
    }

    public static Scopes of(Set<Scope> values) {
        if (values == null || values.isEmpty()) {
            throw new DomainException("Scopes 至少要有一項");
        }
        return new Scopes(new LinkedHashSet<>(values));
    }

    public static Scopes fromStrings(Set<String> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new DomainException("Scopes 至少要有一項");
        }
        return new Scopes(raw.stream().map(Scope::of)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /** this 是否「包含」other 的全部 scope（即 other ⊆ this）。 */
    public boolean containsAll(Scopes other) {
        return this.values.containsAll(other.values);
    }

    public boolean contains(Scope scope) {
        return values.contains(scope);
    }

    public Set<Scope> asSet() {
        return Collections.unmodifiableSet(values);
    }

    public Set<String> asStringSet() {
        return values.stream().map(Scope::value)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Scopes other && values.equals(other.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
