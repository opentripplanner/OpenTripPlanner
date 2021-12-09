package org.opentripplanner.smoketest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.api.json.JSONObjectMapperProvider;

/**
 * This is both a utility class and a category to select or deselect smoke tests during test
 * execution.
 * <p>
 * By default, the smoke tests are not run when you execute `mvn test`.
 * <p>
 * If you want run them use the following command: `mvn test -P smoke-tests`
 */
public class SmokeTest {

    static final ObjectMapper mapper;

    static {
        var provider = new JSONObjectMapperProvider();
        mapper = provider.getContext(null);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
    }

    static LocalDate nextMonday() {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }


    static HttpRequest planRequest(Map<String, String> params) {
        var urlParams = params.entrySet()
                .stream()
                .map(kv -> kv.getKey() + "=" + kv.getValue())
                .collect(Collectors.joining("&"));

        var uri = URI.create("http://localhost:8080/otp/routers/default/plan?" + urlParams);

        return HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

    }


}
