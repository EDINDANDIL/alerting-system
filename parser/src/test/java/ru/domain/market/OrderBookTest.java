package ru.domain.market;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.domain.agent.Trader;
import ru.domain.agent.TraderType;
import ru.domain.simulation.SimulationContext;
import ru.dto.TradeTick;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    private OrderBook orderBook;
    private Trader dummyTrader;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook("BTCUSD");
        dummyTrader = new Trader() {
            @Override
            public long id() {
                return 1L;
            }

            @Override
            public TraderType type() {
                return TraderType.NOISE;
            }

            @Override
            public List<Order> tick(Exchange exchange, SimulationContext context) {
                return List.of();
            }

            @Override
            public void onOrderFilled(Order order, long price, long quantity) {}
        };
    }

    @Test
    void initialOrderBookIsEmpty() {
        assertEquals(0, orderBook.getBestBid());
        assertEquals(0, orderBook.getBestAsk());
    }

    @Test
    void limitOrderBuy_addsToBids() {
        Order order = new Order(dummyTrader, Type.LIMIT, Side.BUY, "BTCUSD", 100L, 5L);
        List<TradeTick> trades = orderBook.order(order);

        assertTrue(trades.isEmpty());
        assertEquals(100L, orderBook.getBestBid());
        assertEquals(0L, orderBook.getBestAsk());
    }

    @Test
    void limitOrderSell_addsToAsks() {
        Order order = new Order(dummyTrader, Type.LIMIT, Side.SELL, "BTCUSD", 110L, 5L);
        List<TradeTick> trades = orderBook.order(order);

        assertTrue(trades.isEmpty());
        assertEquals(0L, orderBook.getBestBid());
        assertEquals(110L, orderBook.getBestAsk());
    }

    @Test
    void matchingLimitOrders_executesTrades() {
        Order sellOrder = new Order(dummyTrader, Type.LIMIT, Side.SELL, "BTCUSD", 100L, 10L);
        orderBook.order(sellOrder);

        Order buyOrder = new Order(dummyTrader, Type.LIMIT, Side.BUY, "BTCUSD", 100L, 7L);
        List<TradeTick> trades = orderBook.order(buyOrder);

        assertEquals(1, trades.size());
        assertEquals(100L, trades.get(0).price());
        assertTrue(buyOrder.isFilled());
        assertEquals(3L, sellOrder.getCount());
        assertEquals(100L, orderBook.getBestAsk());
        assertEquals(0L, orderBook.getBestBid());
    }

    @Test
    void marketOrder_matchesAgainstBook() {
        Order sellLimit = new Order(dummyTrader, Type.LIMIT, Side.SELL, "BTCUSD", 100L, 10L);
        orderBook.order(sellLimit);

        Order buyMarket = new Order(dummyTrader, Type.MARKET, Side.BUY, "BTCUSD", 0L, 5L);
        List<TradeTick> trades = orderBook.order(buyMarket);

        assertEquals(1, trades.size());
        assertEquals(100L, trades.get(0).price());
        assertTrue(buyMarket.isFilled());
        assertEquals(5L, sellLimit.getCount());
    }

    @Test
    void cancelOrder_removesFromBook() {
        Order buyOrder = new Order(dummyTrader, Type.LIMIT, Side.BUY, "BTCUSD", 100L, 5L);
        orderBook.order(buyOrder);
        assertEquals(100L, orderBook.getBestBid());

        orderBook.cancel(buyOrder);
        assertEquals(0L, orderBook.getBestBid());
    }
}
