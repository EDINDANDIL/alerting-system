package ru.alertcli.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Запрос на создание/удаление фильтра для filter-service.
 * POST/DELETE <a href="http://localhost:8081/api/filters">...</a>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FilterRequest {
    @JsonProperty("action")
    private String action = "IMPULSE";

    @JsonProperty("exchange")
    private List<String> exchange;

    @JsonProperty("market")
    private List<String> market;

    @JsonProperty("blackList")
    private List<String> blackList;

    @JsonProperty("timeWindow")
    private int timeWindow = 60;

    @JsonProperty("direction")
    private String direction;

    @JsonProperty("percent")
    private double percent;

    @JsonProperty("volume24h")
    private long volume24h = 0;

    public FilterRequest() {}

    public FilterRequest(List<String> exchange, List<String> market, String direction, double percent) {
        this.exchange = exchange;
        this.market = market;
        this.direction = direction;
        this.percent = percent;
        this.blackList = List.of();
    }

    // Getters and Setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public List<String> getExchange() { return exchange; }
    public void setExchange(List<String> exchange) { this.exchange = exchange; }

    public List<String> getMarket() { return market; }
    public void setMarket(List<String> market) { this.market = market; }

    public List<String> getBlackList() { return blackList; }
    public void setBlackList(List<String> blackList) { this.blackList = blackList; }

    public int getTimeWindow() { return timeWindow; }
    public void setTimeWindow(int timeWindow) { this.timeWindow = timeWindow; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public double getPercent() { return percent; }
    public void setPercent(double percent) { this.percent = percent; }

    public long getVolume24h() { return volume24h; }
    public void setVolume24h(long volume24h) { this.volume24h = volume24h; }
}
