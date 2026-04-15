package ru.core.util;

import ru.tinkoff.kora.common.Component;

import java.util.Set;

// TODO прописать планировщик для сбора монет
@Component
public final class MonetStore {

    private final Set<String> symbols = Set.of("BST", "ETH");

    public Set<String> getSymbols() {
        return symbols;
    }
}
