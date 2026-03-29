package ru.alertcli.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.alertcli.model.FilterRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * HTTP клиент для взаимодействия с filter-service.
 * POST/DELETE <a href="http://localhost:8081/api/filters">...</a>
 */
public class FilterHttpClient {
    private final HttpClient httpClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public FilterHttpClient(String baseUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper();
    }


    public boolean createFilter(int userId, FilterRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/filters"))
                    .header("Content-Type", "application/json")
                    .header("X-user-id", String.valueOf(userId))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Filter created successfully");
                return true;
            } else {
                System.err.println("Failed to create filter: HTTP " + response.statusCode());
                System.err.println("   Response: " + response.body());
                return false;
            }
        } catch (IOException e) {
            System.err.println("HTTP error: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Request interrupted");
            return false;
        }
    }

    public boolean deleteFilter(int userId, FilterRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/filters"))
                    .header("Content-Type", "application/json")
                    .header("X-user-id", String.valueOf(userId))
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Filter deleted successfully");
                return true;
            } else {
                System.err.println(" Failed to delete filter: HTTP " + response.statusCode());
                System.err.println("   Response: " + response.body());
                return false;
            }
        } catch (IOException e) {
            System.err.println(" HTTP error: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(" Request interrupted");
            return false;
        }
    }

    public String getFilters(int userId) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/filters?userId=" + userId))
                    .header("X-user-id", String.valueOf(userId))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println(" Failed to get filters: HTTP " + response.statusCode());
                return null;
            }
        } catch (IOException e) {
            System.err.println(" HTTP error: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(" Request interrupted");
            return null;
        }
    }
}
