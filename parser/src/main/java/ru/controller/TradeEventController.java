package ru.controller;

import ru.service.TradeEventGenerator;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;
import ru.tinkoff.kora.http.common.body.HttpBody;
import ru.tinkoff.kora.http.server.common.HttpServerRequest;
import ru.tinkoff.kora.http.server.common.HttpServerResponse;
import ru.tinkoff.kora.http.server.common.annotation.HttpController;

@Component
@HttpController
public class TradeEventController {

    private final TradeEventGenerator generator;

    public TradeEventController(TradeEventGenerator generator) {
        this.generator = generator;
    }

    @HttpRoute(method = HttpMethod.POST, path = "/api/trades/generate")
    public HttpServerResponse generate(HttpServerRequest request) {
        int count = parseCount(request);
        var result = generator.generate(count);

        String body = """
                {"status":"%s","count":%d,"lastPrice":%d,"minPrice":%d,"maxPrice":%d,"totalVolume":%d}
                """.formatted(
                        result.status(),
                        result.count(),
                        result.lastPrice(),
                        result.minPrice(),
                        result.maxPrice(),
                        result.totalVolume()
                );

        return HttpServerResponse.of(200, HttpBody.plaintext(body));
    }

    @HttpRoute(method = HttpMethod.GET, path = "/api/trades/status")
    public HttpServerResponse status(HttpServerRequest request) {
        String body = """
                {"generating":%b,"generatedCount":%d}
                """.formatted(
                        generator.isGenerating(),
                        generator.getGeneratedCount()
                );

        return HttpServerResponse.of(200, HttpBody.plaintext(body));
    }

    private int parseCount(HttpServerRequest request) {
        try {
            String body = request.body().toString();
            int idx = body.indexOf("\"count\"");
            if (idx >= 0) {
                int colonIdx = body.indexOf(':', idx);
                int endIdx = body.indexOf('}', colonIdx);
                if (colonIdx > 0 && endIdx > colonIdx) {
                    String val = body.substring(colonIdx + 1, endIdx).trim();
                    return Integer.parseInt(val);
                }
            }
        } catch (Exception ignored) {
        }
        return 100000;
    }
}
