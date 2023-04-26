package org.opentripplanner.updater.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpHeadersTest {

  @Test
  void empty() {
    assertEquals(Map.of(), HttpHeaders.empty().asMap());
  }

  @Test
  void acceptApplicationXML() {
    var subject = HttpHeaders.of().acceptApplicationXML();
    var expected = Map.of("Accept", "application/xml");
    assertEquals(expected, subject.build().asMap());
  }

  @Test
  void headersMapBuild() {
    var subject = HttpHeaders.of(Map.of("test", "value"));
    var expected = Map.of("test", "value");
    assertEquals(expected, subject.asMap());

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

  @Test
  void testCombine() {
    var h1 = HttpHeaders.of().acceptProtobuf();
    var h2 = HttpHeaders.of().add("foo", "bar").build();

    var combined = h1.add(h2).build().asMap();

    assertEquals(
      Map.of(
        "Accept",
        "application/x-google-protobuf, application/x-protobuf, application/protobuf, application/octet-stream, */*",
        "foo",
        "bar"
      ),
      combined
    );
  }

  @Test
  void testOverride() {
    var h1 = HttpHeaders.of().acceptProtobuf();
    var h2 = HttpHeaders.of().add("Accept", "bar").build();

    var combined = h1.add(h2).build().asMap();

    assertEquals(Map.of("Accept", "bar"), combined);
  }
}
