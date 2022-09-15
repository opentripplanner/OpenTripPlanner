package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.JsonSupport.newNodeAdapterForTest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.util.OtpAppException;
import org.slf4j.Logger;

public class NodeAdapterTest {

  public static final Duration D3h = Duration.ofHours(3);

  @Test
  public void testAsRawNode() {
    NodeAdapter subject = newNodeAdapterForTest("{ foo : 'bar' }");
    assertFalse(subject.asRawNode("anObject").has("withText"));
  }

  @Test
  public void isEmpty() {
    NodeAdapter subject = newNodeAdapterForTest("");
    assertTrue(subject.path("alf").isEmpty());

    subject = newNodeAdapterForTest("{}");
    assertTrue(subject.path("alf").isEmpty());
    assertTrue(subject.path("alfa").path("bet").isEmpty());
  }

  @Test
  public void path() {
    NodeAdapter subject = newNodeAdapterForTest("{ foo : 'bar' }");
    assertFalse(subject.path("foo").isEmpty());
    assertTrue(subject.path("missingObject").isEmpty());
  }

  @Test
  public void asBoolean() {
    NodeAdapter subject = newNodeAdapterForTest("{ aBoolean : true }");
    assertTrue(subject.asBoolean("aBoolean", false));
    assertFalse(subject.asBoolean("missingField", false));
  }

  @Test
  public void asDouble() {
    NodeAdapter subject = newNodeAdapterForTest("{ aDouble : 7.0 }");
    assertEquals(7.0, subject.asDouble("aDouble", -1d), 0.01);
    assertEquals(7.0, subject.asDouble("aDouble"), 0.01);
    assertEquals(-1d, subject.asDouble("missingField", -1d), 00.1);
  }

  @Test
  public void asDoubles() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : [ 2.0, 3.0, 5.0 ] }");
    assertEquals(List.of(2d, 3d, 5d), subject.asDoubles("key", null));
  }

  @Test
  public void asInt() {
    NodeAdapter subject = newNodeAdapterForTest("{ aInt : 5 }");
    assertEquals(5, subject.asInt("aInt", -1));
    assertEquals(-1, subject.asInt("missingField", -1));
  }

  @Test
  public void asLong() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : 5 }");
    assertEquals(5, subject.asLong("key", -1));
    assertEquals(-1, subject.asLong("missingField", -1));
  }

  @Test
  public void requiredAsLong() {
    NodeAdapter subject = newNodeAdapterForTest("{ }");
    assertThrows(OtpAppException.class, () -> subject.asLong("missingField"));
  }

  @Test
  public void asText() {
    NodeAdapter subject = newNodeAdapterForTest("{ aText : 'TEXT' }");
    assertEquals("TEXT", subject.asText("aText", "DEFAULT"));
    assertEquals("DEFAULT", subject.asText("missingField", "DEFAULT"));
    assertNull(subject.asText("missingField", null));

    assertEquals("TEXT", subject.asText("aText"));
  }

  @Test
  public void requiredAsText() {
    NodeAdapter subject = newNodeAdapterForTest("{ }");
    assertThrows(OtpAppException.class, () -> subject.asText("missingField"));
  }

  @Test
  public void rawAsText() {
    NodeAdapter subject = newNodeAdapterForTest("{ aText : 'TEXT' }");
    assertEquals("TEXT", subject.path("aText").asText());
  }

  @Test
  public void asEnum() {
    // Given
    NodeAdapter subject = newNodeAdapterForTest("{ key : 'A' }");

    // Then
    assertEquals(AnEnum.A, subject.asEnum("key", AnEnum.B), "Get existing property");
    assertEquals(AnEnum.B, subject.asEnum("missing-key", AnEnum.B), "Get default value");
    assertEquals(AnEnum.A, subject.asEnum("key", AnEnum.class), "Get existing property");
  }

  @Test
  public void asEnumWithIllegalPropertySet() {
    // Given
    NodeAdapter subject = newNodeAdapterForTest("{ key : 'NONE_EXISTING_ENUM_VALUE' }");

    // Then expect an error when value 'NONE_EXISTING_ENUM_VALUE' is not in the set of legal
    // values: ['A', 'B', 'C']
    assertThrows(OtpAppException.class, () -> subject.asEnum("key", AnEnum.B));
  }

  @Test
  public void asEnumMap() {
    // With optional enum values in map
    NodeAdapter subject = newNodeAdapterForTest("{ key : { A: true, B: false } }");
    assertEquals(
      Map.of(AnEnum.A, true, AnEnum.B, false),
      subject.asEnumMap("key", AnEnum.class, NodeAdapter::asBoolean)
    );
    assertEquals(
      Collections.<AnEnum, Boolean>emptyMap(),
      subject.asEnumMap("missing-key", AnEnum.class, NodeAdapter::asBoolean)
    );
  }

  @Test
  public void asEnumMapWithUnknownValue() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : { unknown : 7 } }");
    assertEquals(
      Map.<AnEnum, Double>of(),
      subject.asEnumMap("key", AnEnum.class, NodeAdapter::asDouble)
    );

    // Assert unknown parameter is logged at warning level and with full pathname
    Logger log = Mockito.mock(Logger.class);
    subject.logAllUnusedParameters(log);
    Mockito.verify(log).warn(Mockito.anyString(), Mockito.eq("key.unknown:7"), Mockito.eq("Test"));
  }

  @Test
  public void asEnumMapAllKeysRequired() {
    // Require all enum values to exist (if param exist)
    NodeAdapter subject = newNodeAdapterForTest("{ key : { A: true, B: false, C: true } }");
    assertEquals(
      Map.of(AnEnum.A, true, AnEnum.B, false, AnEnum.C, true),
      subject.asEnumMapAllKeysRequired("key", AnEnum.class, NodeAdapter::asBoolean)
    );
    assertNull(subject.asEnumMapAllKeysRequired("missing-key", AnEnum.class, NodeAdapter::asText));
  }

  @Test
  public void asEnumMapWithRequiredMissingValue() {
    // A value for C is missing in map
    NodeAdapter subject = newNodeAdapterForTest("{ key : { A: true, B: false } }");

    assertThrows(
      OtpAppException.class,
      () -> subject.asEnumMapAllKeysRequired("key", AnEnum.class, NodeAdapter::asBoolean)
    );
  }

  @Test
  public void asEnumSetUsingJsonArray() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : [ 'A', 'B' ] }");
    assertEquals(Set.of(AnEnum.A, AnEnum.B), subject.asEnumSet("key", AnEnum.class));
    assertEquals(Set.of(), subject.asEnumSet("missing-key", AnEnum.class));
  }

  @Test
  public void asEnumSetUsingConcatenatedString() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : 'A,B' }");
    assertEquals(Set.of(AnEnum.A, AnEnum.B), subject.asEnumSet("key", AnEnum.class));
  }

  @Test
  public void asFeedScopedId() {
    NodeAdapter subject = newNodeAdapterForTest("{ key1: 'A:23', key2: 'B:12' }");
    assertEquals("A:23", subject.asFeedScopedId("key1", null).toString());
    assertEquals("B:12", subject.asFeedScopedId("key2", null).toString());
    assertEquals(
      "C:12",
      subject.asFeedScopedId("missing-key", new FeedScopedId("C", "12")).toString()
    );
  }

  @Test
  public void asFeedScopedIds() {
    NodeAdapter subject = newNodeAdapterForTest("{ routes: ['A:23', 'B:12']}");
    assertEquals("[A:23, B:12]", subject.asFeedScopedIds("routes", List.of()).toString());
    assertEquals("[]", subject.asFeedScopedIds("missing-key", List.of()).toString());
    assertEquals(
      "[C:12]",
      subject.asFeedScopedIds("missing-key", List.of(new FeedScopedId("C", "12"))).toString()
    );
  }

  @Test
  public void asFeedScopedIdSet() {
    NodeAdapter subject = newNodeAdapterForTest("{ routes: ['A:23', 'B:12', 'A:23']}");
    assertEquals(
      List.of(
        new FeedScopedId("A", "23"),
        new FeedScopedId("B", "12"),
        new FeedScopedId("A", "23")
      ),
      subject.asFeedScopedIdList("routes", List.of())
    );
  }

  @Test
  public void asDateOrRelativePeriod() {
    // Given
    NodeAdapter subject = newNodeAdapterForTest("{ 'a' : '2020-02-28', 'b' : '-P3Y' }");

    // Then
    assertEquals(LocalDate.of(2020, 2, 28), subject.asDateOrRelativePeriod("a", null));

    assertEquals(LocalDate.now().minusYears(3), subject.asDateOrRelativePeriod("b", null));
    assertEquals(
      LocalDate.of(2020, 3, 1),
      subject.asDateOrRelativePeriod("do-no-exist", "2020-03-01")
    );
    assertNull(subject.asDateOrRelativePeriod("do-no-exist", null));
  }

  @Test
  public void testParsePeriodDateThrowsException() {
    // Given
    NodeAdapter subject = newNodeAdapterForTest("{ 'foo' : 'bar' }");

    // Then
    assertThrows(OtpAppException.class, () -> subject.asDateOrRelativePeriod("foo", null));
  }

  @Test
  public void asDuration() {
    NodeAdapter subject = newNodeAdapterForTest("{ k1:'PT1s', k2:'3h2m1s', k3:7 }");

    // as required duration
    assertEquals("PT1S", subject.asDuration("k1").toString());
    assertEquals("PT3H2M1S", subject.asDuration("k2").toString());

    // as optional duration
    assertEquals("PT1S", subject.asDuration("k1", null).toString());
    assertEquals("PT3H", subject.asDuration("missing-key", D3h).toString());

    // as required duration v2 (with unit)
    assertEquals("PT1S", subject.asDuration("k1").toString());
    assertEquals("PT7S", subject.asDuration2("k3", ChronoUnit.SECONDS).toString());

    // as optional duration v2 (with unit)
    assertEquals("PT1S", subject.asDuration2("k1", null, ChronoUnit.SECONDS).toString());
    assertEquals("PT7S", subject.asDuration2("k3", null, ChronoUnit.SECONDS).toString());
    assertEquals("PT3H", subject.asDuration2("missing-key", D3h, ChronoUnit.SECONDS).toString());
  }

  @Test
  public void requiredAsDuration() {
    NodeAdapter subject = newNodeAdapterForTest("{ }");
    assertThrows(OtpAppException.class, () -> subject.asDuration("missingField"));
  }

  @Test
  public void asDurations() {
    NodeAdapter subject = newNodeAdapterForTest("{ key1 : ['PT1s', '2h'] }");
    assertEquals("[PT1S, PT2H]", subject.asDurations("key1", List.of()).toString());
    assertEquals("[PT3H]", subject.asDurations("missing-key", List.of(D3h)).toString());
  }

  @Test
  public void asLocale() {
    NodeAdapter subject = newNodeAdapterForTest(
      "{ key1 : 'no', key2 : 'no_NO', key3 : 'no_NO_NY' }"
    );
    assertEquals("no", subject.asLocale("key1", null).toString());
    assertEquals("no_NO", subject.asLocale("key2", null).toString());
    assertEquals("no_NO_NY", subject.asLocale("key3", null).toString());
    assertEquals(Locale.FRANCE, subject.asLocale("missing-key", Locale.FRANCE));
  }

  @Test
  public void asPattern() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : 'Ab*a' }");
    assertEquals("Ab*a", subject.asPattern("key", "ABC").toString());
    assertEquals("ABC", subject.asPattern("missingField", "ABC").toString());
  }

  @Test
  public void uri() {
    var URL = "gs://bucket/a.obj";
    NodeAdapter subject = newNodeAdapterForTest("{ aUri : '" + URL + "' }");

    assertEquals(URL, subject.asUri("aUri").toString());
    assertEquals(URL, subject.asUri("aUri", null).toString());
    assertEquals("http://foo.bar/", subject.asUri("missingField", "http://foo.bar/").toString());
    assertNull(subject.asUri("missingField", null));
  }

  @Test
  public void uriSyntaxException() {
    NodeAdapter subject = newNodeAdapterForTest("{ aUri : 'error$%uri' }");

    assertThrows(OtpAppException.class, () -> subject.asUri("aUri", null), "error$%uri");
  }

  @Test
  public void uriRequiredValueMissing() {
    NodeAdapter subject = newNodeAdapterForTest("{ }");

    assertThrows(
      OtpAppException.class,
      () -> subject.asUri("aUri"),
      "Required parameter 'aUri' not found in 'Test'"
    );
  }

  @Test
  public void uris() {
    NodeAdapter subject = newNodeAdapterForTest("{ foo : ['gs://a/b', 'gs://c/d'] }");
    assertEquals("[gs://a/b, gs://c/d]", subject.asUris("foo").toString());

    subject = newNodeAdapterForTest("{ }");
    assertEquals("[]", subject.asUris("foo").toString());
  }

  @Test
  public void urisNotAnArrayException() {
    NodeAdapter subject = newNodeAdapterForTest("{ 'uris': 'no array' }");

    assertThrows(
      OtpAppException.class,
      () -> subject.asUris("uris"),
      "'uris': 'no array'" + "Source: Test"
    );
  }

  @Test
  public void objectAsList() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : [{ a: 'I' }, { a: '2' } ] }");

    List<NodeAdapter> result = subject.path("key").asList();

    String content = result.stream().map(n -> n.asText("a")).collect(Collectors.joining(", "));

    assertEquals("I, 2", content);
  }

  @Test
  public void linearFunction() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : '4+8x' }");
    assertEquals("f(x) = 4.0 + 8.0 x", subject.asLinearFunction("key", null).toString());
    assertNull(subject.asLinearFunction("no-key", null));
  }

  @Test
  public void asZoneId() {
    NodeAdapter subject = newNodeAdapterForTest(
      "{ key1 : 'UTC', key2 : 'Europe/Oslo', key3 : '+02:00', key4: 'invalid' }"
    );
    assertEquals("UTC", subject.asZoneId("key1", null).getId());
    assertEquals("Europe/Oslo", subject.asZoneId("key2", null).getId());
    assertEquals("+02:00", subject.asZoneId("key3", null).getId());

    assertThrows(OtpAppException.class, () -> subject.asZoneId("key4", null));

    assertEquals(ZoneId.of("UTC"), subject.asZoneId("missing-key", ZoneId.of("UTC")));
  }

  @Test
  public void asMap() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : { A: true, B: false } }");
    assertEquals(Map.of("A", true, "B", false), subject.asMap("key", NodeAdapter::asBoolean));
    assertEquals(
      Collections.<String, Boolean>emptyMap(),
      subject.asMap("missing-key", NodeAdapter::asBoolean)
    );
  }

  @Test
  public void asTextSet() {
    NodeAdapter subject = newNodeAdapterForTest("{ ids : ['A', 'C', 'F'] }");
    assertEquals(Set.of("A", "C", "F"), subject.asTextSet("ids", Collections.emptySet()));
    assertEquals(Set.of("X"), subject.asTextSet("nonExisting", Set.of("X")));
  }

  @Test
  public void isNonEmptyArray() {
    NodeAdapter subject = newNodeAdapterForTest("{ foo : ['A'], bar: [], foobar: true }");
    assertTrue(subject.path("foo").isNonEmptyArray());
    assertFalse(subject.path("bar").isNonEmptyArray());
    assertFalse(subject.path("foobar").isNonEmptyArray());
    assertFalse(subject.path("missing").isNonEmptyArray());
  }

  private enum AnEnum {
    A,
    B,
    C,
  }
}
