package ru.flink.operator;

import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import ru.common.dto.OutboxCreatedEvent;
import ru.common.dto.OutboxPayload;
import ru.flink.model.AlertEvent;
import ru.flink.model.RuntimeFilter;
import ru.flink.model.KeyedTradeTick;
import ru.flink.state.PriceWindow;
import ru.flink.strategy.ImpulseStrategy;
import ru.flink.model.TradePoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Set;

public final class AlertProcessFunction extends KeyedBroadcastProcessFunction<
        String,
        KeyedTradeTick,
        OutboxCreatedEvent,
        AlertEvent> {

    private final MapStateDescriptor<Long, RuntimeFilter> filtersDescriptor;

    private transient MapState<Long, PriceWindow> windows;
    private transient ImpulseStrategy impulseStrategy;

    public AlertProcessFunction(MapStateDescriptor<Long, RuntimeFilter> filtersDescriptor) {
        this.filtersDescriptor = filtersDescriptor;
    }

    @Override
    public void open(OpenContext openContext) {
        MapStateDescriptor<Long, PriceWindow> windowsDescriptor =
                new MapStateDescriptor<>(
                        "windows",
                        Long.class,
                        PriceWindow.class
                );

        windows = getRuntimeContext().getMapState(windowsDescriptor);
        impulseStrategy = new ImpulseStrategy();
    }

    @Override
    public void processBroadcastElement(
            OutboxCreatedEvent event,
            Context ctx,
            Collector<AlertEvent> out
    ) throws Exception {
        BroadcastState<Long, RuntimeFilter> filters =
                ctx.getBroadcastState(filtersDescriptor);

        switch (event.operation()) {


            case CREATE -> {
                if (event.payload() instanceof OutboxPayload.ImpulseFilter payload) {
                    filters.put(
                            event.filterId(),
                            new RuntimeFilter(event.filterId(), payload, Set.of())
                    );
                }
            }


            case DELETE -> filters.remove(event.filterId());


            case SUBSCRIBE -> {
                RuntimeFilter old = filters.get(event.filterId());
                if (old != null) {
                    filters.put(event.filterId(), old.subscribe(event.userId()));
                }
            }


            case UNSUBSCRIBE -> {
                RuntimeFilter old = filters.get(event.filterId());
                if (old != null) {
                    filters.put(event.filterId(), old.unsubscribe(event.userId()));
                }
            }


        }
    }

    @Override
    public void processElement(
            KeyedTradeTick tick,
            ReadOnlyContext ctx,
            Collector<AlertEvent> out
    ) throws Exception {
        ReadOnlyBroadcastState<Long, RuntimeFilter> filters =
                ctx.getBroadcastState(filtersDescriptor);

        Map<Long, List<RuntimeFilter>> filtersByWindow = new HashMap<>();

        for (Map.Entry<Long, RuntimeFilter> entry : filters.immutableEntries()) {
            RuntimeFilter filter = entry.getValue();

            if (filter.subscribers().isEmpty()) continue;

            if (filter.payload().blackList() != null
                && filter.payload().blackList().contains(tick.symbol())) {
                continue;
            }

            long windowNs = filter.payload().timeWindow() * 1_000_000_000L;
            filtersByWindow
                    .computeIfAbsent(windowNs, ignored -> new ArrayList<>())
                    .add(filter);
        }

        for (Map.Entry<Long, List<RuntimeFilter>> entry : filtersByWindow.entrySet()) {
            long windowNs = entry.getKey();

            PriceWindow window = windows.get(windowNs);

            if (window == null) window = new PriceWindow(windowNs);

            window.add(new TradePoint(tick.timestampNs(), tick.price()));
            windows.put(windowNs, window);

            for (RuntimeFilter filter : entry.getValue()) {

                if (!impulseStrategy.trigger(window, filter)) continue;

                out.collect(new AlertEvent(
                        filter.subscribers(),
                        filter.payload().exchange(),
                        filter.payload().market(),
                        tick.symbol(),
                        tick.timestampNs()
                ));
            }
        }
    }
}