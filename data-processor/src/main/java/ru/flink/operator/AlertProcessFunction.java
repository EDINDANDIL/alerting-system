package ru.flink.operator;

import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import ru.common.dto.AlertCreatedEvent;
import ru.common.dto.FilterCreatedEvent;
import ru.common.dto.OutboxPayload;
import ru.flink.models.ImpulseRuntimeFilter;
import ru.flink.models.KeyedTradeTick;
import ru.flink.state.SlidingPriceWindow;
import ru.flink.strategy.ImpulseStrategy;
import ru.flink.models.TradePoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Set;

public final class AlertProcessFunction extends KeyedBroadcastProcessFunction<
        String,
        KeyedTradeTick,
        FilterCreatedEvent,
        AlertCreatedEvent> {

    private final MapStateDescriptor<Long, ImpulseRuntimeFilter> filtersDescriptor;

    private transient MapState<Long, SlidingPriceWindow> windows;
    private transient ImpulseStrategy impulseStrategy;

    public AlertProcessFunction(MapStateDescriptor<Long, ImpulseRuntimeFilter> filtersDescriptor) {
        this.filtersDescriptor = filtersDescriptor;
    }

    @Override
    public void open(OpenContext openContext) {
        MapStateDescriptor<Long, SlidingPriceWindow> windowsDescriptor =
                new MapStateDescriptor<>(
                        "windows",
                        Long.class,
                        SlidingPriceWindow.class
                );

        windows = getRuntimeContext().getMapState(windowsDescriptor);
        impulseStrategy = new ImpulseStrategy();
    }

    @Override
    public void processBroadcastElement(
            FilterCreatedEvent event,
            Context ctx,
            Collector<AlertCreatedEvent> out
    ) throws Exception {
        BroadcastState<Long, ImpulseRuntimeFilter> filters =
                ctx.getBroadcastState(filtersDescriptor);

        switch (event.operation()) {


            case CREATE -> {
                if (event.payload() instanceof OutboxPayload.ImpulseFilter payload) {
                    filters.put(
                            event.filterId(),
                            new ImpulseRuntimeFilter(event.filterId(), payload, Set.of())
                    );
                }
            }


            case DELETE -> filters.remove(event.filterId());


            case SUBSCRIBE -> {
                ImpulseRuntimeFilter old = filters.get(event.filterId());
                if (old != null) {
                    filters.put(event.filterId(), old.subscribe(event.userId()));
                }
            }


            case UNSUBSCRIBE -> {
                ImpulseRuntimeFilter old = filters.get(event.filterId());
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
            Collector<AlertCreatedEvent> out
    ) throws Exception {
        ReadOnlyBroadcastState<Long, ImpulseRuntimeFilter> filters =
                ctx.getBroadcastState(filtersDescriptor);

        Map<Long, List<ImpulseRuntimeFilter>> filtersByWindow = new HashMap<>();

        for (Map.Entry<Long, ImpulseRuntimeFilter> entry : filters.immutableEntries()) {
            ImpulseRuntimeFilter filter = entry.getValue();

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

        for (Map.Entry<Long, List<ImpulseRuntimeFilter>> entry : filtersByWindow.entrySet()) {
            long windowNs = entry.getKey();

            SlidingPriceWindow window = windows.get(windowNs);

            if (window == null) window = new SlidingPriceWindow(windowNs);

            window.add(new TradePoint(tick.timestampNs(), tick.price()));
            windows.put(windowNs, window);

            for (ImpulseRuntimeFilter filter : entry.getValue()) {

                if (!impulseStrategy.trigger(window, filter)) continue;

                out.collect(new AlertCreatedEvent(
                        filter.filterId(),
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
