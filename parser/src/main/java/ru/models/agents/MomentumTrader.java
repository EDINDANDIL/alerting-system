package ru.models.agents;

import ru.models.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MomentumTrader extends AbstractTrader {
    private final double alpha; // Скорость затухания тренда (0.001 для LMT / 0.9 для SMT)
    private final double beta;  // Коэффициент спроса
    private final double gamma; // Насыщение (обычно 10.0)
    private final double rho;   // Соотношение рынок/лимит
    private final double muL;
    private final double sigmaL;
    private final List<String> symbols; // Список торгуемых монет

    // Состояние тренда и цен изолировано по каждой монете
    private final Map<String, Double> mtMap = new HashMap<>();
    private final Map<String, Long> lastPriceMap = new HashMap<>();

    public MomentumTrader(double theta, double mu, double delta, long balance, 
                          double alpha, double beta, double gamma, double rho, double muL, double sigmaL,
                          List<String> symbols) {
        super(theta, mu, delta, balance);
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        this.rho = rho;
        this.muL = muL;
        this.sigmaL = sigmaL;
        this.symbols = symbols;
    }

    @Override
    public List<Order> tick(Exchange market, long currentTick) {
        List<Order> newOrders = new ArrayList<>();
        for (String symbol : symbols) {
            newOrders.addAll(tickForSymbol(market, symbol, currentTick));
        }
        return newOrders;
    }

    private List<Order> tickForSymbol(Exchange market, String symbol, long currentTick) {
        List<Order> newOrders = new ArrayList<>();
        long pt = market.getMarketPrice(symbol);

        // 1. Отмена старых ордеров для данного символа
        orders().removeIf(order -> {
            if (order.getSymbol().equals(symbol) && random.nextDouble() < getDelta()) {
                market.cancel(order);
                return true;
            }
            return false;
        });

        if (pt == 0) return newOrders; // Если торгов еще не было, тренд не считаем

        // Рассчитываем объем ордера (целевой объем сделки $5,000)
        long quantity = Math.max(1, Math.round(5000.0 / ((double) pt / 100_000_000.0)));
        
        // 2. Расчет тренда Mt для конкретной монеты (на основе процентного изменения цены)
        double mt = mtMap.getOrDefault(symbol, 0.0);
        long lastPrice = lastPriceMap.getOrDefault(symbol, 0L);

        if (lastPrice == 0) {
            lastPrice = pt;
        } else {
            mt = (1 - alpha) * mt + alpha * ((double) (pt - lastPrice) / lastPrice);
            lastPrice = pt;
        }
        mtMap.put(symbol, mt);
        lastPriceMap.put(symbol, lastPrice);

        // 3. Расчет вероятностей на основе гиперболического тангенса
        // Так как доходности малы (например, 0.01 за шаг = 1%), масштабируем mt на 100,
        // чтобы demand соответствовал масштабу исходной калибровки в долларовом пространстве.
        double demand = beta * Math.tanh(gamma * mt * 100.0);
        double currentTheta = Math.abs(demand);
        double currentMu = currentTheta * rho;

        Side side = mt >= 0 ? Side.BUY : Side.SELL;

        // 4. Генерация лимитного ордера
        if (random.nextDouble() < currentTheta) {
            double x = muL + sigmaL * random.nextGaussian();
            // Масштабируем абсолютную дистанцию ($1 в калибровке) пропорционально цене актива
            long distance = Math.round(Math.exp(x) * pt / 50000.0);
            long price = (side == Side.BUY) ? (pt - distance) : (pt + distance);

            // Округляем до tickSize
            long tickSize = market.getTickSize(symbol);
            price = Math.round((double) price / tickSize) * tickSize;
            
            Order limitOrder = new Order(this, Type.LIMIT, side, symbol, price, quantity);
            addOrder(limitOrder);
            newOrders.add(limitOrder);
        }

        // 5. Генерация рыночного ордера
        if (random.nextDouble() < currentMu) {
            Order marketOrder = new Order(this, Type.MARKET, side, symbol, 0, quantity);
            newOrders.add(marketOrder);
        }

        return newOrders;
    }
}