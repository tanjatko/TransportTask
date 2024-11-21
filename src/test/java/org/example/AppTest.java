package org.example;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class AppTest {

    @Test
    public void shouldRepeatApplication()  {
        //String token = System.getenv("token");

        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        RequestSpecBuilder builder = new RequestSpecBuilder();
        builder.setBaseUri("https://api.lardi-trans.com/v2");
        builder.addHeader("Authorization", "2NK0IAW8LP2000004004");
        builder.addHeader("Accept", "application/json");

        RequestSpecification spec = builder.build();

        given().spec(spec).when()
                .get("/proposals/my/lorries/published").then()
                .statusCode(200);

        given().spec(spec).contentType("application/json").body("{\"lorryIds\":[297563298516]}").when()
                .post("/proposals/my/repeat/").then()
                .statusCode(200);
    }
}
