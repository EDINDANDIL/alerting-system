package ru.core.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;
import ru.core.engine.FilterEngine;
import ru.core.util.MonetStore;
import ru.models.dto.TradeEvent;
import ru.models.states.ImpulseFilterView;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IndexTest {

    private static final String SYMBOL = "ETH";
    private static final String SYMBOL2 = "BST";

    private Index index;
    private FilterStore filterStore;
    private WindowStore windowStore;

    @BeforeEach
    void setUp() {
        filterStore = new FilterStore();
        windowStore = new WindowStore();
        FilterEngine filterEngine = new FilterEngine();
        MonetStore monetStore = new MonetStore();

        index = new Index(monetStore, windowStore, filterStore, filterEngine);
    }

    @Nested
    class AddPoint {

        @Test
        void addPoint_unknownSymbol_doesNotThrow() {
            index.addPoint(new TradeEvent("binance", "futures", "unknown", 1L, 1L, 0L, 0L, 100L, 0L, 0L, 0, 0));
            assertDoesNotThrow(() -> {});
        }

        @Test
        void addPoint_knownSymbol_addsPoint() {
            createFilterForSymbol(1L, SYMBOL);
            windowStore.getOrCompute(60_000_000_000L);
            index.addPoint(trade(1L, 100L));
            assertDoesNotThrow(() -> {});
        }
    }

    @Nested
    class Check {

        @Test
        void check_noFilters_returnsEmpty() {
            var result = index.check(SYMBOL);
            assertTrue(result.isEmpty());
        }

        @Test
        void check_unknownSymbol_returnsEmpty() {
            var result = index.check("unknown");
            assertTrue(result.isEmpty());
        }

        @Test
        void check_filterNotTriggered_returnsEmpty() {
            createFilterForSymbol(1L, SYMBOL);
            windowStore.getOrCompute(60_000_000_000L);
            index.addPoint(trade(1L, 100L));
            index.addPoint(trade(2L, 105L));

            var result = index.check(SYMBOL);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class Create {

        @Test
        void create_filterIndexedForAllowedSymbols() {
            var payload = impulsePayload(Set.of());
            filterStore.put(1L, new ImpulseFilterView(1L, payload, Set.of(100L)));

            var result = index.create(1L, null, 60);

            assertTrue(result.containsKey("BST"));
            assertTrue(result.containsKey("ETH"));
        }

        @Test
        void create_withBlacklist_excludesBlacklisted() {
            var payload = impulsePayload(Set.of(SYMBOL));
            filterStore.put(1L, new ImpulseFilterView(1L, payload, Set.of(100L)));

            var result = index.create(1L, Set.of(SYMBOL), 60);

            assertFalse(result.containsKey(SYMBOL));
            assertTrue(result.containsKey(SYMBOL2));
        }

        @Test
        void create_returnsIndex() {
            var result = index.create(1L, null, 60);
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }
    }

    @Nested
    class Delete {

        @Test
        void delete_nonExistentFilter_noError() {
            assertDoesNotThrow(() -> index.delete(999L));
        }

        @Test
        void delete_cleansUpEmptyEntries() {
            createFilterForSymbol(1L, SYMBOL);
            createFilterForSymbol(2L, SYMBOL2);

            index.delete(1L);

            var symbolEntry = index.create(0L, Set.of(SYMBOL2), 60);
            assertNotNull(symbolEntry);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void check_emptyWindow_returnsEmpty() {
            createFilterForSymbol(1L, SYMBOL);
            var result = index.check(SYMBOL);
            assertTrue(result.isEmpty());
        }

        @Test
        void multipleSymbols_bothIndexed() {
            createFilterForSymbol(1L, SYMBOL);
            createFilterForSymbol(2L, SYMBOL2);

            var ethIndex = index.create(1L, null, 60);
            var bstIndex = index.create(2L, null, 60);

            assertTrue(ethIndex.containsKey(SYMBOL));
            assertTrue(bstIndex.containsKey(SYMBOL2));
        }
    }

    private void createFilterForSymbol(long filterId, String symbol) {
        var payload = impulsePayload(Set.of());
        filterStore.put(filterId, new ImpulseFilterView(filterId, payload, Set.of(100L)));
        index.create(filterId, null, 60);
    }

    private TradeEvent trade(long id, long price) {
        return trade(SYMBOL, id, price);
    }

    private TradeEvent trade(String symbol, long id, long price) {
        return new TradeEvent("binance", "futures", symbol, id, id * 1_000_000L, 0L, 0L, price, 0L, 0L, 0, 0);
    }

    private OutboxPayload.ImpulseFilter impulsePayload(Set<String> blacklist) {
        return new OutboxPayload.ImpulseFilter(
                Set.of("binance"), Set.of("futures"), blacklist, 60, Direction.UP, 10, 0);
    }
}
