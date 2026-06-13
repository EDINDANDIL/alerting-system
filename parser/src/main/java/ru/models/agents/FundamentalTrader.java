package ru.models.agents;

import ru.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FundamentalTrader extends AbstractTrader {
    private final double kappa1;     // Коэффициент линейного спроса
    private final double kappa2;     // Коэффициент кубического спроса
    private final long interval;     // Как часто ходит (например, 100 тиков)
    private final List<String> symbols; // Монеты
    private final Map<String, Long> targetValues; // Справедливая цена Vt по каждой монете

    public FundamentalTrader(long balance, double kappa1, double kappa2, long interval, 
                             List<String> symbols, Map<String, Long> targetValues) {
        super(0.0, 0.0, 0.0, balance); // Лимиток нет (theta = 0, delta = 0)
        this.kappa1 = kappa1;
        this.kappa2 = kappa2;
        this.interval = interval;
        this.symbols = symbols;
        this.targetValues = targetValues;
    }

    @Override
    public List<Order> tick(Exchange market, long currentTick) {
        List<Order> newOrders = new ArrayList<>();

        // Проверяем интервал хода
        if (currentTick % interval != 0) {
            return newOrders;
        }

        for (String symbol : symbols) {
            Long targetValue = targetValues.get(symbol);
            if (targetValue == null) continue;

            long pt = market.getMarketPrice(symbol);
            if (pt == 0) continue;

            // Рассчитываем объем ордера (целевой объем сделки $5,000)
            long quantity = Math.max(1, Math.round(5000.0 / ((double) pt / 100_000_000.0)));

            // Переводим разницу цен в относительные проценты перед расчетом спроса,
            // чтобы логика оставалась независимой от абсолютной цены монеты.
            double diff = ((double) (targetValue - pt) / targetValue) * 100.0;
            
            // Расчет кубического спроса
            double demand = kappa1 * diff + kappa2 * Math.pow(diff, 3);
            double currentMu = Math.abs(demand);

            if (random.nextDouble() < currentMu) {
                Side side = diff > 0 ? Side.BUY : Side.SELL;
                Order marketOrder = new Order(this, Type.MARKET, side, symbol, 0, quantity);
                newOrders.add(marketOrder);
            }
        }

        return newOrders;
    }
}