package org.opentripplanner.updater.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OtpAppException;

class HttpHeadersTest {

  @Test
  void empty() {
    assertEquals(Map.of(), HttpHeaders.empty().headers());
  }

  @Test
  void acceptApplicationXML() {
    var subject = HttpHeaders.of("Test").acceptApplicationXML();
    var expected = Map.of("Accept", "application/xml");
    assertEquals(expected, subject.build().headers());
  }

  @Test
  void headersMapBuild() {
    var subject = HttpHeaders.of(Map.of("test", "value"), "Test");
    var expected = Map.of("test", "value");
    assertEquals(expected, subject.headers());

    subject = HttpHeaders.of(Map.of(), "Test");
    assertEquals(HttpHeaders.empty(), subject);
  }

  @Test
  void assertEnvSubstitution() {
    var key = "testKey";
    String value = "UUID='${UUID}'";
    var subject = HttpHeaders.of("Test").add(key, value).build();
    String result = subject.headers().get(key);
    assertNotEquals(value, result);
    assertTrue(result.matches("UUID='[-\\da-f]+'"), result);
  }

  @Test
  void assertEnvSubstitutionFailsOnMissingKey() {
    assertThrows(
      OtpAppException.class,
      () -> HttpHeaders.of("Test").add("test", "${NONE_EXISTING_ENV_VARIABLE_d8j9r0X}")
    );
  }

  @Test
  void testToString() {
    var subject = HttpHeaders.of("Test").add("test", "value").build();
    assertEquals("HttpHeaders{test=value}", subject.toString());
  }

  @Test
  void testEqAmdHashCode() {
    var subject = HttpHeaders.of("Test").add("test", "value").build();
    var same = HttpHeaders.of("Test").add("test", "value").build();
    var other = HttpHeaders.of("Test").add("test", "x").build();

    assertEquals(same, subject);
    assertEquals(same.hashCode(), subject.hashCode());
    assertNotEquals(other, subject);
    assertNotEquals(other.hashCode(), subject.hashCode());
  }
}
