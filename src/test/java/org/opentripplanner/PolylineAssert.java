package org.opentripplanner;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PolylineAssert {

    public static String makeUrl(String line) {
        return "https://leonard.io/polyline-visualiser/?base64=" + toBase64(line);
    }

    public static void assertThatPolylinesAreEqual(String actual, String expected) {

        var reason = "Actual polyline is not equal to the expected one. View them on a map: \n" +
                "Expected:  " + makeUrl(expected) + "\n" +
                "Actual:    " + makeUrl(actual) + "\n";
        assertThat(reason, actual, is(expected));
    }

    private static String toBase64(String line) {
        return Base64.getUrlEncoder().encodeToString(line.getBytes(StandardCharsets.UTF_8));
    }
}
