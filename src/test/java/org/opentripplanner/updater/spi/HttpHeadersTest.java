package org.opentripplanner.updater.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpHeadersTest {

  @Test
  void empty() {
    assertEquals(Map.of(), HttpHeaders.empty().headers());
  }

  @Test
  void acceptApplicationXML() {
    var subject = HttpHeaders.of().acceptApplicationXML();
    var expected = Map.of("Accept", "application/xml");
    assertEquals(expected, subject.build().headers());
  }

  @Test
  void headersMapBuild() {
    var subject = HttpHeaders.of(Map.of("test", "value"));
    var expected = Map.of("test", "value");
    assertEquals(expected, subject.headers());

    subject = HttpHeaders.of(Map.of());
    assertEquals(HttpHeaders.empty(), subject);
  }

  @Test
  void testToString() {
    var subject = HttpHeaders.of().add("test", "value").build();
    assertEquals("HttpHeaders{test=value}", subject.toString());
  }

  @Test
  void testEqAndHashCode() {
    var subject = HttpHeaders.of().add("test", "value").build();
    var same = HttpHeaders.of().add("test", "value").build();
    var other = HttpHeaders.of().add("test", "x").build();

    assertEquals(same, subject);
    assertEquals(same.hashCode(), subject.hashCode());
    assertNotEquals(other, subject);
    assertNotEquals(other.hashCode(), subject.hashCode());
  }
}
