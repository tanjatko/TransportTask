package org.example;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.assertNotNull;

/**
 * Re-publishes ("repeats") every lorry proposal that is currently published on Lardi-Trans.
 * API docs: https://api.lardi-trans.com/v2/docs/my-proposals/repeat.html
 */
public class AppTest {

    private static final String BASE_URI = "https://api.lardi-trans.com/v2";
    private static final int PAGE_SIZE = 100;

    @Test
    public void shouldRepeatPublishedLorries() {
        RequestSpecification spec = buildSpec(readToken());

        List<Long> lorryIds = fetchPublishedLorryIds(spec);
        if (lorryIds.isEmpty()) {
            System.out.println("No published lorry proposals found - nothing to repeat.");
            return;
        }
        System.out.println("Repeating lorry proposals: " + lorryIds);

        JsonPath result = given().spec(spec)
                .body("{\"lorryIds\":" + lorryIds + "}")
                .when()
                .post("/proposals/my/repeat")
                .then()
                .statusCode(200)
                .extract().jsonPath();

        List<Object> success = result.getList("lorries.success");
        List<Map<String, Object>> errors = result.getList("lorries.errors");

        System.out.println("Repeated successfully: " + success);
        if (errors != null && !errors.isEmpty()) {
            // Lardi refusing a repeat (e.g. too soon) is expected with frequent runs:
            // show it as a warning on the run instead of failing it.
            System.out.println("::warning title=Not repeated::" + errors);
        }

        assertNotNull(success, "Unexpected response: no lorries.success list");
    }

    private static String readToken() {
        String token = System.getenv("token");
        if (token == null || token.isBlank()) {
            token = System.getProperty("token");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Lardi-Trans API token is missing: set the 'token' environment variable (GitHub secret TOKEN).");
        }
        return token.trim();
    }

    private static RequestSpecification buildSpec(String token) {
        // Log requests and responses, but never print the Authorization header.
        RestAssuredConfig config = RestAssuredConfig.config()
                .logConfig(LogConfig.logConfig().blacklistHeader("Authorization"));

        return new RequestSpecBuilder()
                .setBaseUri(BASE_URI)
                .setConfig(config)
                .addHeader("Authorization", token)
                .addHeader("Accept", "application/json")
                .setContentType("application/json")
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();
    }

    private static List<Long> fetchPublishedLorryIds(RequestSpecification spec) {
        List<Long> ids = new ArrayList<>();
        int page = 1;
        int pages;
        do {
            JsonPath body = given().spec(spec)
                    .queryParam("page", page)
                    .queryParam("size", PAGE_SIZE)
                    .when()
                    .get("/proposals/my/lorries/published")
                    .then()
                    .statusCode(200)
                    .extract().jsonPath();

            // IDs exceed int range, so read them as raw values and parse as long.
            List<Object> pageIds = body.getList("content.id");
            if (pageIds != null) {
                pageIds.forEach(id -> ids.add(Long.parseLong(String.valueOf(id))));
            }
            Object totalPages = body.get("paginator.pages");
            pages = totalPages == null ? 1 : Integer.parseInt(String.valueOf(totalPages));
            page++;
        } while (page <= pages);
        return ids;
    }
}
