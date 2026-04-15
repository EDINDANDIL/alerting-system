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
        // 1. Query param: POST /api/trades/generate?count=500
        var queryParams = request.queryParams();
        var countValues = queryParams.get("count");
        if (countValues != null && !countValues.isEmpty()) {
            String countParam = countValues.iterator().next();
            if (countParam != null && !countParam.isEmpty()) {
                try {
                    int v = Integer.parseInt(countParam);
                    System.out.println("[parseCount] From query: " + v);
                    return v;
                } catch (NumberFormatException e) {
                    System.err.println("[parseCount] Invalid query param count: " + countParam);
                }
            }
        }

        // 2. JSON body: {"count": 500}
        try {
            var body = request.body();
            java.io.InputStream is = body.asInputStream();
            byte[] bytes = is.readAllBytes();
            if (bytes != null && bytes.length > 0) {
                String bodyStr = new String(bytes);
                System.out.println("[parseCount] Body: " + bodyStr);
                int idx = bodyStr.indexOf("\"count\"");
                if (idx >= 0) {
                    int colonIdx = bodyStr.indexOf(':', idx);
                    int endIdx = bodyStr.indexOf(',', colonIdx);
                    if (endIdx < 0) endIdx = bodyStr.indexOf('}', colonIdx);
                    if (colonIdx > 0 && endIdx > colonIdx) {
                        String val = bodyStr.substring(colonIdx + 1, endIdx).trim();
                        int v = Integer.parseInt(val);
                        System.out.println("[parseCount] From body: " + v);
                        return v;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[parseCount] Body parse error: " + e.getMessage());
        }

        // 3. Default
        System.out.println("[parseCount] Default: 100000");
        return 100000;
    }
}
