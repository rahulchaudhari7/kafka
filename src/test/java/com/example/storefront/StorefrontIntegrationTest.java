package com.example.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StorefrontIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void servesStorefrontAndItsAssets() throws Exception {
        HttpResponse<String> page = get("/");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("thoughtfully chosen");

        HttpResponse<String> stylesheet = get("/styles.css");
        assertThat(stylesheet.statusCode()).isEqualTo(200);
        assertThat(stylesheet.headers().firstValue("content-type").orElse(""))
                .contains("text/css");

        HttpResponse<String> script = get("/app.js");
        assertThat(script.statusCode()).isEqualTo(200);
        assertThat(script.body()).contains("/api/products");
    }

    @Test
    void exposesHealthAndCatalogEndpoints() throws Exception {
        JsonNode health = objectMapper.readTree(get("/actuator/health").body());
        assertThat(health.path("status").asText()).isEqualTo("UP");

        JsonNode readiness = objectMapper.readTree(get("/actuator/health/readiness").body());
        assertThat(readiness.path("status").asText()).isEqualTo("UP");

        HttpResponse<String> response = get("/api/products");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode products = objectMapper.readTree(response.body());
        assertThat(products).hasSize(2);
        assertThat(products.get(0).path("name").asText()).isEqualTo("Notebook");
        assertThat(products.get(0).path("price").asDouble()).isEqualTo(4.5);

        HttpResponse<String> item = get("/api/products/1");
        assertThat(item.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(item.body()).path("name").asText())
                .isEqualTo("Notebook");
    }

    @Test
    void unknownProductsReturnTheContractError() throws Exception {
        HttpResponse<String> response = get("/api/products/999");
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(objectMapper.readTree(response.body()).path("error").asText())
                .isEqualTo("Product not found");
    }

    @Test
    void productListSatisfiesTheConsumerContract() throws Exception {
        JsonNode contract;
        try (var input = new ClassPathResource("storefront-products.v1.json").getInputStream()) {
            contract = objectMapper.readTree(input);
        }

        HttpResponse<String> response = get(contract.path("request").path("path").asText());
        assertThat(response.statusCode())
                .isEqualTo(contract.path("response").path("status").asInt());

        JsonNode schema = contract.path("response").path("body").path("items");
        Set<String> required = StreamSupport.stream(schema.path("required").spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toSet());
        JsonNode expectedTypes = schema.path("types");
        JsonNode products = objectMapper.readTree(response.body());
        assertThat(products.isArray()).isTrue();
        for (JsonNode product : products) {
            for (String field : required) {
                assertThat(product.has(field)).as("required field %s", field).isTrue();
            }
            expectedTypes.fields().forEachRemaining(entry -> {
                boolean matchesType = switch (entry.getValue().asText()) {
                    case "number" -> product.path(entry.getKey()).isNumber();
                    case "string" -> product.path(entry.getKey()).isTextual();
                    case "boolean" -> product.path(entry.getKey()).isBoolean();
                    default -> false;
                };
                assertThat(matchesType).as("field %s has the expected type", entry.getKey())
                        .isTrue();
            });
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
