package org.opentripplanner.standalone.config.framework.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.JsonSupport.newNodeAdapterForTest;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.util.OtpAppException;

public class NodeAdapterTest {

  public static final Duration D3h = Duration.ofHours(3);

  @Test
  public void testAsRawNode() {
    NodeAdapter subject = newNodeAdapterForTest("{ foo : 'bar' }");
    assertFalse(subject.rawNode("anObject").has("withText"));
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
    assertTrue(subject.of("aBoolean").withDoc(NA, /*TODO DOC*/"TODO").asBoolean());
    assertTrue(subject.of("aBoolean").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false));
    assertFalse(subject.of("missingField").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false));
  }

  @Test
  public void asDouble() {
    NodeAdapter subject = newNodeAdapterForTest("{ aDouble : 7.0 }");
    assertEquals(7.0, subject.of("aDouble").withDoc(NA, /*TODO DOC*/"TODO").asDouble(-1d), 0.01);
    assertEquals(7.0, subject.of("aDouble").withDoc(NA, /*TODO DOC*/"TODO").asDouble(), 0.01);
    assertEquals(
      -1d,
      subject.of("missingField").withDoc(NA, /*TODO DOC*/"TODO").asDouble(-1d),
      00.1
    );
  }

  @Test
  public void asDoubles() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : [ 2.0, 3.0, 5.0 ] }");
    assertEquals(
      List.of(2d, 3d, 5d),
      subject.of("key").withDoc(NA, /*TODO DOC*/"TODO").asDoubles(null)
    );
  }

  @Test
  public void asInt() {
    NodeAdapter subject = newNodeAdapterForTest("{ aInt : 5 }");
    assertEquals(5, subject.of("aInt").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1));
    assertEquals(-1, subject.of("missingField").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1));
  }

  @Test
  public void asLong() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : 5 }");
    assertEquals(5, subject.of("key").withDoc(NA, /*TODO DOC*/"TODO").asLong(-1));
    assertEquals(-1, subject.of("missingField").withDoc(NA, /*TODO DOC*/"TODO").asLong(-1));
  }

  @Test
  public void asText() {
    NodeAdapter subject = newNodeAdapterForTest("{ aText : 'TEXT' }");
    assertEquals(
      "TEXT",
      subject
        .of("aText")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString("DEFAULT")
    );
    assertEquals(
      "DEFAULT",
      subject
        .of("missingField")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString("DEFAULT")
    );
    assertNull(
      subject
        .of("missingField")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString(null)
    );

    assertEquals(
      "TEXT",
      subject.of("aText").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString()
    );
  }

  @Test
  public void requiredAsText() {
    NodeAdapter subject = newNodeAdapterForTest("{ }");
    assertThrows(
      OtpAppException.class,
      () ->
        subject
          .of("missingField")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asString()
    );
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
    assertEquals(
      AnEnum.A,
      subject.of("key").withDoc(NA, /*TODO DOC*/"TODO").asEnum(AnEnum.B),
      "Get existing property"
    );
    assertEquals(
      AnEnum.B,
      subject.of("missing-key").withDoc(NA, /*TODO DOC*/"TODO").asEnum(AnEnum.B),
      "Get default value"
    );
    assertEquals(
      AnEnum.A,
      subject.of("key").withDoc(NA, /*TODO DOC*/"TODO").asEnum(AnEnum.class),
      "Get existing property"
    );
  }

  @Test
  public void asEnumWithIllegalPropertySet() {
    // Given
    NodeAdapter subject = newNodeAdapterForTest("{ key : 'NONE_EXISTING_ENUM_VALUE' }");

    // Then expect an error when value 'NONE_EXISTING_ENUM_VALUE' is not in the set of legal
    // values: ['A', 'B', 'C']
    assertThrows(
      OtpAppException.class,
      () -> subject.of("key").withDoc(NA, /*TODO DOC*/"TODO").asEnum(AnEnum.B)
    );
  }

  @Test
  public void asEnumMap() {
    // With optional enum values in map
    NodeAdapter subject = newNodeAdapterForTest("{ key : { A: true, B: false } }");
    assertEquals(
      Map.of(AnEnum.A, true, AnEnum.B, false),
      subject
        .of("key")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asEnumMap(AnEnum.class, Boolean.class)
    );
    assertEquals(
      Collections.<AnEnum, Boolean>emptyMap(),
      subject
        .of("missing-key")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asEnumMap(AnEnum.class, Boolean.class)
    );
  }

  @Test
  public void asEnumMapWithUnknownValue() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : { unknown : 7 } }");
    assertEquals(
      Map.<AnEnum, Double>of(),
      subject
        .of("key")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asEnumMap(AnEnum.class, Double.class)
    );

    // Assert unknown parameter is logged at warning level and with full pathname
    var buf = new StringBuilder();
    subject.logAllUnusedParameters(buf::append);
    assertEquals("Unexpected config parameter: 'key.unknown:7' in 'Test'", buf.toString());
  }

  @Test
  public void asEnumMapAllKeysRequired() {
    // Require all enum values to exist (if param exist)
    NodeAdapter subject = newNodeAdapterForTest("{ key : { A: true, B: false, C: true } }");
    assertEquals(
      Map.of(AnEnum.A, true, AnEnum.B, false, AnEnum.C, true),
      subject
        .of("key")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asEnumMapAllKeysRequired(AnEnum.class, Boolean.class)
    );
    assertNull(
      subject
        .of("missing-key")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asEnumMapAllKeysRequired(AnEnum.class, Boolean.class)
    );
  }

  @Test
  public void asEnumMapWithRequiredMissingValue() {
    // A value for C is missing in map
    NodeAdapter subject = newNodeAdapterForTest("{ key : { A: true, B: false } }");

    assertThrows(
      OtpAppException.class,
      () ->
        subject
          .of("key")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asEnumMapAllKeysRequired(AnEnum.class, Boolean.class)
    );
  }

  @Test
  public void asEnumSet() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : [ 'A', 'B' ] }");
    assertEquals(
      Set.of(AnEnum.A, AnEnum.B),
      subject
        .of("key")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asEnumSet(AnEnum.class)
    );
    assertEquals(
      Set.of(),
      subject
        .of("missing-key")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asEnumSet(AnEnum.class)
    );
  }

  @Test
  public void asEnumSetFailsUsingWrongFormat() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : 'A,B' }");
    assertThrows(
      OtpAppException.class,
      () ->
        subject
          .of("key")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asEnumSet(AnEnum.class)
    );
  }

  @Test
  public void asFeedScopedId() {
    NodeAdapter subject = newNodeAdapterForTest("{ key1: 'A:23', key2: 'B:12' }");
    assertEquals(
      "A:23",
      subject.of("key1").withDoc(NA, /*TODO DOC*/"TODO").asFeedScopedId(null).toString()
    );
    assertEquals(
      "B:12",
      subject.of("key2").withDoc(NA, /*TODO DOC*/"TODO").asFeedScopedId(null).toString()
    );
    assertEquals(
      "C:12",
      subject
        .of("missing-key")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asFeedScopedId(new FeedScopedId("C", "12"))
        .toString()
    );
  }

  @Test
  public void asFeedScopedIds() {
    NodeAdapter subject = newNodeAdapterForTest("{ routes: ['A:23', 'B:12']}");
    assertEquals(
      "[A:23, B:12]",
      subject.of("routes").withDoc(NA, /*TODO DOC*/"TODO").asFeedScopedIds(List.of()).toString()
    );
    assertEquals(
      "[]",
      subject
        .of("missing-key")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asFeedScopedIds(List.of())
        .toString()
    );
    assertEquals(
      "[C:12]",
      subject
        .of("missing-key")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asFeedScopedIds(List.of(new FeedScopedId("C", "12")))
        .toString()
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
      subject.of("routes").withDoc(NA, /*TODO DOC*/"TODO").asFeedScopedIds(List.of())
    );
  }

  @Test
  public void asDateOrRelativePeriod() {
    // Given
    var subject = newNodeAdapterForTest("{ 'a' : '2020-02-28', 'b' : '-P3Y' }");
    var utc = ZoneId.of("UTC");

    // Then
    assertEquals(
      LocalDate.of(2020, 2, 28),
      subject.of("a").withDoc(NA, /*TODO DOC*/"TODO").asDateOrRelativePeriod(null, utc)
    );

    assertEquals(
      LocalDate.now().minusYears(3),
      subject.of("b").withDoc(NA, /*TODO DOC*/"TODO").asDateOrRelativePeriod(null, utc)
    );
    assertEquals(
      LocalDate.of(2020, 3, 1),
      subject
        .of("do-no-exist")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asDateOrRelativePeriod("2020-03-01", utc)
    );
    assertNull(
      subject.of("do-no-exist").withDoc(NA, /*TODO DOC*/"TODO").asDateOrRelativePeriod(null, utc)
    );
  }

  @Test
  public void testParsePeriodDateThrowsException() {
    // Given
    NodeAdapter subject = newNodeAdapterForTest("{ 'foo' : 'bar' }");

    // Then
    assertThrows(
      OtpAppException.class,
      () ->
        subject
          .of("foo")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDateOrRelativePeriod(null, ZoneId.systemDefault())
    );
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
    assertEquals(
      "no",
      subject.of("key1").withDoc(NA, /*TODO DOC*/"TODO").asLocale(null).toString()
    );
    assertEquals(
      "no_NO",
      subject.of("key2").withDoc(NA, /*TODO DOC*/"TODO").asLocale(null).toString()
    );
    assertEquals(
      "no_NO_NY",
      subject.of("key3").withDoc(NA, /*TODO DOC*/"TODO").asLocale(null).toString()
    );
    assertEquals(
      Locale.FRANCE,
      subject.of("missing-key").withDoc(NA, /*TODO DOC*/"TODO").asLocale(Locale.FRANCE)
    );
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

    List<ARecord> result = subject
      .of("key")
      .withDoc(V2_0, "Summary Array")
      .asObjects(
        List.of(),
        n -> new ARecord(n.of("a").withDoc(V2_1, "Summary Element").asString())
      );

    assertEquals("[ARecord[a=I], ARecord[a=2]]", result.toString());
    assertEquals("[key : object[] = [] Since 2.0]", subject.parametersSorted().toString());
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
  public void asTextSet() {
    NodeAdapter subject = newNodeAdapterForTest("{ ids : ['A', 'C', 'F'] }");
    assertEquals(
      Set.of("A", "C", "F"),
      Set.copyOf(
        subject
          .of("ids")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asStringList(List.copyOf(Collections.emptySet()))
      )
    );
    assertEquals(
      Set.of("X"),
      Set.copyOf(
        subject
          .of("nonExisting")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asStringList(List.copyOf(Set.of("X")))
      )
    );
  }

  @Test
  public void isNonEmptyArray() {
    NodeAdapter subject = newNodeAdapterForTest("{ foo : ['A'], bar: [], foobar: true }");
    assertTrue(subject.path("foo").isNonEmptyArray());
    assertFalse(subject.path("bar").isNonEmptyArray());
    assertFalse(subject.path("foobar").isNonEmptyArray());
    assertFalse(subject.path("missing").isNonEmptyArray());
  }

  @Test
  public void deduplicateChildren() {
    NodeAdapter subject = newNodeAdapterForTest("{ foo : { enabled: true } }");
    assertSame(subject.path("foo"), subject.path("foo"));
  }

  @Test
  public void unusedParams() {
    // Given: two parameters a and b
    NodeAdapter subject = newNodeAdapterForTest("{ foo : { a: true, b: false } }");
    var buf = new StringBuilder();

    // When: Access ONLY parameter 'a', but not 'b'
    assertTrue(subject.path("foo").of("a").withDoc(NA, /*TODO DOC*/"TODO").asBoolean());

    // Then: expect 'b' to be unused
    subject.logAllUnusedParameters(buf::append);
    assertEquals("Unexpected config parameter: 'foo.b:false' in 'Test'", buf.toString());
  }

  private enum AnEnum {
    A,
    B,
    C,
  }

  private record ARecord(String a) {}
}
