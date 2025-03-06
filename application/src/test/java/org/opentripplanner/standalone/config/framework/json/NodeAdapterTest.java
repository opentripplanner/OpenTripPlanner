package org.opentripplanner.standalone.config.framework.json;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.BOOLEAN;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_4;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class NodeAdapterTest {

  public static final Duration D3h = Duration.ofHours(3);
  public static final String NON_UNUSED_PARAMETERS = "EXPECTED_NONE";

  @Test
  void testAsRawNode() {
    NodeAdapter subject = newNodeAdapterForTest("{ child : { foo : 'bar' } }");

    // Define child
    var child = subject.of("child").asObject();

    // Retrieve child as raw node
    assertEquals("{\"foo\":\"bar\"}", child.rawNode().toString());

    // Both the root(subject) and the child should report an empty list of unused parameters
    var up = new ArrayList<>();
    subject.logAllWarnings(up::add);
    assertEquals(List.of(), up);

    child.logAllWarnings(up::add);
    assertEquals(List.of(), up);
  }

  @Test
  void isEmpty() {
    NodeAdapter subject = newNodeAdapterForTest("");
    assertTrue(subject.of("alf").asObject().isEmpty());

    subject = newNodeAdapterForTest("{}");
    assertTrue(subject.of("alf").asObject().isEmpty());
    assertTrue(subject.of("alfa").asObject().of("bet").asObject().isEmpty());
    assertEquals(NON_UNUSED_PARAMETERS, unusedParams(subject));
  }

  @Test
  void path() {
    NodeAdapter subject = newNodeAdapterForTest("{ foo : 'bar' }");
    assertFalse(subject.of("foo").asObject().isEmpty());
    assertTrue(subject.of("missingObject").asObject().isEmpty());
    assertEquals(NON_UNUSED_PARAMETERS, unusedParams(subject));
  }

  @Test
  void docInfo() {
    NodeAdapter subject = newNodeAdapterForTest("{ bool: false }");
    subject.of("bool").since(V2_0).summary("B Summary").description("Ddd").asBoolean();
    subject.of("en").since(V2_1).summary("EN Summary").asEnum(SECONDS);
    subject.of("em").since(V2_1).summary("EM Summary").asEnumMap(ChronoUnit.class, String.class);

    List<NodeInfo> infos = subject.parametersSorted();
    assertEquals(
      "[" +
      "bool : boolean Required Since 2.0, " +
      "en : enum = \"seconds\" Since 2.1, " +
      "em : enum map of string Optional Since 2.1" +
      "]",
      infos.toString()
    );
    assertEquals("bool", infos.get(0).name());
    assertNull(infos.get(0).defaultValue());
    assertEquals("B Summary", infos.get(0).summary());
    assertEquals("Ddd", infos.get(0).description());
    assertEquals(BOOLEAN, infos.get(0).type());
  }

  @Test
  void asBoolean() {
    NodeAdapter subject = newNodeAdapterForTest("{ aBoolean : true }");
    assertTrue(subject.of("aBoolean").asBoolean());
    assertTrue(subject.of("aBoolean").asBoolean(false));
    assertFalse(subject.of("missingField").asBoolean(false));
  }

  @Test
  void asDouble() {
    NodeAdapter subject = newNodeAdapterForTest("{ aDouble : 7.0 }");
    assertEquals(7.0, subject.of("aDouble").asDouble(-1d), 0.01);
    assertEquals(7.0, subject.of("aDouble").asDouble(), 0.01);
    assertEquals(-1d, subject.of("missingField").asDouble(-1d), 00.1);
  }

  @Test
  void asDoubles() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : [ 2.0, 3.0, 5.0 ] }");
    assertEquals(List.of(2d, 3d, 5d), subject.of("key").asDoubles(null));
    assertEquals(NON_UNUSED_PARAMETERS, unusedParams(subject));
  }

  @Test
  void asInt() {
    NodeAdapter subject = newNodeAdapterForTest("{ aInt : 5 }");
    assertEquals(5, subject.of("aInt").asInt(-1));
    assertEquals(-1, subject.of("missingField").asInt(-1));
  }

  @Test
  void asLong() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : 5 }");
    assertEquals(5, subject.of("key").asLong(-1));
    assertEquals(-1, subject.of("missingField").asLong(-1));
  }

  @Test
  void asText() {
    NodeAdapter subject = newNodeAdapterForTest("{ aText : 'TEXT' }");
    assertEquals("TEXT", subject.of("aText").asString("DEFAULT"));
    assertEquals("DEFAULT", subject.of("missingField").asString("DEFAULT"));
    assertNull(subject.of("missingField").asString(null));

    assertEquals("TEXT", subject.of("aText").asString());
  }

  @Test
  void requiredAsText() {
    NodeAdapter subject = newNodeAdapterForTest("{ }");
    assertThrows(OtpAppException.class, () -> subject.of("missingField").asString());
  }

  @Test
  void rawAsText() {
    NodeAdapter subject = newNodeAdapterForTest("{ aText : 'TEXT' }");
    assertEquals("TEXT", subject.of("aText").asObject().asText());
  }

  @Test
  void asEnum() {
    // Given
    NodeAdapter subject = newNodeAdapterForTest("{ a : 'A', abc : 'a-b-c' }");

    // Then with defaults
    assertEquals(AnEnum.A, subject.of("a").asEnum(AnEnum.B), "Get existing property");
    assertEquals(AnEnum.A_B_C, subject.of("abc").asEnum(AnEnum.A_B_C), "Get existing property");
    assertEquals(AnEnum.B, subject.of("missing-key").asEnum(AnEnum.B), "Get default value");
    // Then required
    assertEquals(AnEnum.A, subject.of("a").asEnum(AnEnum.class), "Get existing property");
    assertEquals(AnEnum.A_B_C, subject.of("abc").asEnum(AnEnum.A_B_C), "Get existing property");
    assertThrows(OtpAppException.class, () -> subject.of("missing-key").asEnum(AnEnum.class));
  }

  @Test
  void asEnumWithIllegalPropertySet() {
    // Given
    NodeAdapter subject = newNodeAdapterForTest(
      """
        {
          key : 'NONE_EXISTING_ENUM_VALUE',
          skim : {
            albin : 'NONE_EXISTING_ENUM_VALUE'
          }
        }
      """
    );

    NodeAdapter child = subject.of("skim").asObject();

    // Then expect an error when value 'NONE_EXISTING_ENUM_VALUE' is not in the set of legal
    // values: ['A', 'B', 'C']
    assertEquals(AnEnum.B, subject.of("key").asEnum(AnEnum.B));
    assertEquals(AnEnum.A, child.of("albin").asEnum(AnEnum.A));

    // Verify logging
    final StringBuilder log = new StringBuilder();
    subject.logAllWarnings(m -> log.append(m).append('\n'));
    assertEquals(
      """
      {error-message} Parameter: skim.albin. Source: Test.
      {error-message} Parameter: key. Source: Test.
      """.replace(
          "{error-message}",
          "The enum value 'NONE_EXISTING_ENUM_VALUE' is not legal. Expected one of [A, B, A_B_C]."
        ),
      log.toString()
    );
  }

  @Test
  void asEnumMap() {
    // With optional enum values in map
    NodeAdapter subject = newNodeAdapterForTest("{ key : { A: true, B: false } }");
    assertEquals(
      Map.of(AnEnum.A, true, AnEnum.B, false),
      subject.of("key").asEnumMap(AnEnum.class, Boolean.class)
    );
    assertEquals(
      Collections.<AnEnum, Boolean>emptyMap(),
      subject.of("missing-key").asEnumMap(AnEnum.class, Boolean.class)
    );
    assertEquals(NON_UNUSED_PARAMETERS, unusedParams(subject));
  }

  @Test
  void asEnumMapWithCustomType() {
    // With optional enum values in map
    NodeAdapter subject = newNodeAdapterForTest("{ key : { A: {a:'Foo'} } }");
    assertEquals(
      Map.of(AnEnum.A, new ARecord("Foo")),
      subject.of("key").asEnumMap(AnEnum.class, ARecord::fromJson, Map.of())
    );
    assertEquals(
      Collections.<AnEnum, Boolean>emptyMap(),
      subject.of("missing-key").asEnumMap(AnEnum.class, ARecord::fromJson, Map.of())
    );
    assertEquals(NON_UNUSED_PARAMETERS, unusedParams(subject));
  }

  @Test
  void asEnumMapWithDefaultValue() {
    var subject = newNodeAdapterForTest("{}");
    final Map<AnEnum, ARecord> dflt = Map.of(AnEnum.A, new ARecord("Foo"));
    assertEquals(dflt, subject.of("key").asEnumMap(AnEnum.class, ARecord::fromJson, dflt));
    assertEquals(NON_UNUSED_PARAMETERS, unusedParams(subject));
  }

  @Test
  void asEnumMapWithUnknownKey() {
    NodeAdapter subject = newNodeAdapterForTest("{ enumMap : { unknown : 7 } }");

    subject.of("enumMap").asEnumMap(AnEnum.class, Double.class);

    final StringBuilder log = new StringBuilder();
    subject.logAllWarnings(m -> log.append(m).append("\n"));

    assertEquals(
      """
      Unexpected config parameter: 'enumMap.unknown:7' in 'Test'
      The enum value 'unknown' is not legal. Expected one of [A, B, A_B_C]. Parameter: enumMap. Source: Test.
      """.stripIndent(),
      log.toString()
    );
  }

  @Test
  void asEnumMapAllKeysRequired() {
    var subject = newNodeAdapterForTest("{ key : { A: true, b: false, a_B_c: true } }");
    assertEquals(
      Map.of(AnEnum.A, true, AnEnum.B, false, AnEnum.A_B_C, true),
      subject.of("key").asEnumMapAllKeysRequired(AnEnum.class, Boolean.class)
    );
    assertNull(subject.of("missing-key").asEnumMapAllKeysRequired(AnEnum.class, Boolean.class));

    var subjectMissingB = newNodeAdapterForTest("{ key : { A: true, a_B_c: true } }");
    assertThrows(OtpAppException.class, () ->
      subjectMissingB.of("key").asEnumMapAllKeysRequired(AnEnum.class, Boolean.class)
    );

    // Any extra keys should be ignored for forward/backward compatibility
    newNodeAdapterForTest("{ key : { A: true, b: false, a_B_c: true, extra: true } }");
  }

  @Test
  void asEnumMapWithRequiredMissingValue() {
    // A value for C is missing in map
    NodeAdapter subject = newNodeAdapterForTest("{ key : { A: true, B: false } }");

    assertThrows(OtpAppException.class, () ->
      subject.of("key").asEnumMapAllKeysRequired(AnEnum.class, Boolean.class)
    );
  }

  @Test
  void asEnumSet() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : [ 'A', 'B' ] }");
    assertEquals(Set.of(AnEnum.A, AnEnum.B), subject.of("key").asEnumSet(AnEnum.class));
    assertEquals(Set.of(), subject.of("missing-key").asEnumSet(AnEnum.class));
    assertEquals(NON_UNUSED_PARAMETERS, unusedParams(subject));
  }

  @Test
  void asEnumSetFailsUsingWrongFormat() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : 'A,B' }");
    assertThrows(OtpAppException.class, () -> subject.of("key").asEnumSet(AnEnum.class));
  }

  @Test
  void asFeedScopedId() {
    NodeAdapter subject = newNodeAdapterForTest("{ key1: 'A:23', key2: 'B:12' }");
    assertEquals("A:23", subject.of("key1").asFeedScopedId(null).toString());
    assertEquals("B:12", subject.of("key2").asFeedScopedId(null).toString());
    assertEquals(
      "C:12",
      subject.of("missing-key").asFeedScopedId(new FeedScopedId("C", "12")).toString()
    );
  }

  @Test
  void asFeedScopedIds() {
    NodeAdapter subject = newNodeAdapterForTest("{ routes: ['A:23', 'B:12']}");
    assertEquals("[A:23, B:12]", subject.of("routes").asFeedScopedIds(List.of()).toString());
    assertEquals("[]", subject.of("missing-key").asFeedScopedIds(List.of()).toString());
    assertEquals(
      "[C:12]",
      subject.of("missing-key").asFeedScopedIds(List.of(new FeedScopedId("C", "12"))).toString()
    );
    assertEquals(NON_UNUSED_PARAMETERS, unusedParams(subject));
  }

  @Test
  void asFeedScopedIdSet() {
    NodeAdapter subject = newNodeAdapterForTest("{ routes: ['A:23', 'B:12', 'A:23']}");
    assertEquals(
      List.of(
        new FeedScopedId("A", "23"),
        new FeedScopedId("B", "12"),
        new FeedScopedId("A", "23")
      ),
      subject.of("routes").asFeedScopedIds(List.of())
    );
    assertEquals(NON_UNUSED_PARAMETERS, unusedParams(subject));
  }

  @Test
  void asDateOrRelativePeriod() {
    // Given
    var subject = newNodeAdapterForTest("{ 'a' : '2020-02-28', 'b' : '-P3Y' }");
    var utc = ZoneIds.UTC;

    // Then
    assertEquals(LocalDate.of(2020, 2, 28), subject.of("a").asDateOrRelativePeriod(null, utc));

    assertEquals(
      LocalDate.now(utc).minusYears(3),
      subject.of("b").asDateOrRelativePeriod(null, utc)
    );
    assertEquals(
      LocalDate.of(2020, 3, 1),
      subject.of("do-no-exist").asDateOrRelativePeriod("2020-03-01", utc)
    );
    assertNull(subject.of("do-no-exist").asDateOrRelativePeriod(null, utc));
  }

  @Test
  void testParsePeriodDateThrowsException() {
    // Given
    NodeAdapter subject = newNodeAdapterForTest("{ 'foo' : 'bar' }");

    // Then
    assertThrows(OtpAppException.class, () ->
      subject.of("foo").asDateOrRelativePeriod(null, ZoneId.systemDefault())
    );
  }

  @Test
  void asDuration() {
    NodeAdapter subject = newNodeAdapterForTest("{ k1:'PT1s', k2:'3h2m1s', k3:7 }");

    // as required duration
    assertEquals("PT1S", subject.of("k1").asDuration().toString());
    assertEquals("PT3H2M1S", subject.of("k2").asDuration().toString());

    // as optional duration
    assertEquals("PT1S", subject.of("k1").asDuration(null).toString());
    assertEquals("PT3H", subject.of("missing-key").asDuration(D3h).toString());

    // as required duration v2 (with unit)
    assertEquals("PT1S", subject.of("k1").asDuration().toString());
  }

  @Test
  void requiredAsDuration() {
    NodeAdapter subject = newNodeAdapterForTest("{ }");
    assertThrows(OtpAppException.class, () -> subject.of("missingField").asDuration());
  }

  @Test
  void asDurations() {
    NodeAdapter subject = newNodeAdapterForTest("{ key1 : ['PT1s', '2h'] }");
    assertEquals("[PT1S, PT2H]", subject.of("key1").asDurations(List.of()).toString());
    assertEquals("[PT3H]", subject.of("missing-key").asDurations(List.of(D3h)).toString());
    assertEquals(NON_UNUSED_PARAMETERS, unusedParams(subject));
  }

  @Test
  void asLocale() {
    NodeAdapter subject = newNodeAdapterForTest(
      "{ key1 : 'no', key2 : 'no_NO', key3 : 'no_NO_NY' }"
    );
    assertEquals("no", subject.of("key1").asLocale(null).toString());
    assertEquals("no_NO", subject.of("key2").asLocale(null).toString());
    assertEquals("no_NO_NY", subject.of("key3").asLocale(null).toString());
    assertEquals(Locale.FRANCE, subject.of("missing-key").asLocale(Locale.FRANCE));
  }

  @Test
  void asPattern() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : 'Ab*a' }");
    assertEquals("Ab*a", subject.of("key").asPattern("ABC").toString());
    assertEquals("ABC", subject.of("missingField").asPattern("ABC").toString());
  }

  @Test
  void uri() {
    var URL = "gs://bucket/a.obj";
    NodeAdapter subject = newNodeAdapterForTest("{ aUri : '" + URL + "' }");

    assertEquals(URL, subject.of("aUri").asUri().toString());
    assertEquals(URL, subject.of("aUri").asUri(null).toString());
    assertEquals("http://foo.bar/", subject.of("missingField").asUri("http://foo.bar/").toString());
    assertNull(subject.of("missingField").asUri(null));
  }

  @Test
  void uriSyntaxException() {
    NodeAdapter subject = newNodeAdapterForTest("{ aUri : 'error$%uri' }");

    assertThrows(OtpAppException.class, () -> subject.of("aUri").asUri(null), "error$%uri");
  }

  @Test
  void uriRequiredValueMissing() {
    NodeAdapter subject = newNodeAdapterForTest("{ }");

    assertThrows(
      OtpAppException.class,
      () -> subject.of("aUri").asUri(),
      "Required parameter 'aUri' not found in 'Test'"
    );
  }

  @Test
  void uris() {
    NodeAdapter subject = newNodeAdapterForTest("{ foo : ['gs://a/b', 'gs://c/d'] }");
    assertEquals("[gs://a/b, gs://c/d]", subject.of("foo").asUris().toString());

    subject = newNodeAdapterForTest("{ }");
    assertEquals("[]", subject.of("foo").asUris().toString());
    assertEquals(NON_UNUSED_PARAMETERS, unusedParams(subject));
  }

  @Test
  void urisNotAnArrayException() {
    NodeAdapter subject = newNodeAdapterForTest("{ 'uris': 'no array' }");

    assertThrows(
      OtpAppException.class,
      () -> subject.of("uris").asUris(),
      "'uris': 'no array'" + "Source: Test"
    );
  }

  @Test
  void objectAsList() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : [{ a: 'I' }, { a: '2' } ] }");

    List<ARecord> result = subject
      .of("key")
      .since(V2_0)
      .summary("Summary Array")
      .asObjects(List.of(), ARecord::fromJson);

    assertEquals("[ARecord[a=I], ARecord[a=2]]", result.toString());
    assertEquals("[key : object[] = [] Since 2.0]", subject.parametersSorted().toString());
    assertEquals(NON_UNUSED_PARAMETERS, unusedParams(subject));
  }

  @Test
  void asCostLinearFunction() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : '400+8x' }");
    assertEquals("6m40s + 8.0 t", subject.of("key").asCostLinearFunction(null).toString());
    assertNull(subject.of("no-key").asCostLinearFunction(null));
  }

  @Test
  void asTimePenalty() {
    NodeAdapter subject = newNodeAdapterForTest("{ key : '400+8x' }");
    assertEquals("6m40s + 8.0 t", subject.of("key").asTimePenalty(null).toString());
    assertNull(subject.of("no-key").asTimePenalty(null));
  }

  @Test
  void asZoneId() {
    NodeAdapter subject = newNodeAdapterForTest(
      "{ key1 : 'UTC', key2 : 'Europe/Oslo', key3 : '+02:00', key4: 'invalid' }"
    );
    assertEquals("UTC", subject.of("key1").asZoneId(null).getId());
    assertEquals("Europe/Oslo", subject.of("key2").asZoneId(null).getId());
    assertEquals("+02:00", subject.of("key3").asZoneId(null).getId());

    assertThrows(OtpAppException.class, () -> subject.of("key4").asZoneId(null));

    assertEquals(ZoneIds.UTC, subject.of("missing-key").asZoneId(ZoneIds.UTC));
  }

  @Test
  void asTextSet() {
    NodeAdapter subject = newNodeAdapterForTest("{ ids : ['A', 'C', 'F'] }");
    assertEquals(
      Set.of("A", "C", "F"),
      Set.copyOf(subject.of("ids").asStringList(List.copyOf(Collections.emptySet())))
    );
    assertEquals(
      Set.of("X"),
      Set.copyOf(subject.of("nonExisting").asStringList(List.copyOf(Set.of("X"))))
    );
  }

  @Test
  void isNonEmptyArray() {
    NodeAdapter subject = newNodeAdapterForTest("{ foo : ['A'], bar: [], foobar: true }");
    assertTrue(subject.of("foo").asObject().isNonEmptyArray());
    assertFalse(subject.of("bar").asObject().isNonEmptyArray());
    assertFalse(subject.of("foobar").asObject().isNonEmptyArray());
    assertFalse(subject.of("missing").asObject().isNonEmptyArray());
    assertEquals(NON_UNUSED_PARAMETERS, unusedParams(subject));
  }

  @Test
  void deduplicateChildren() {
    NodeAdapter subject = newNodeAdapterForTest("{ foo : { enabled: true } }");
    assertSame(subject.of("foo").asObject(), subject.of("foo").asObject());
  }

  @Test
  void unusedParams() {
    // Given: two parameters a and b
    NodeAdapter subject = newNodeAdapterForTest("{ foo : { a: true, b: false } }");
    var buf = new StringBuilder();

    // When: Access ONLY parameter 'a', but not 'b'
    assertTrue(subject.of("foo").asObject().of("a").asBoolean());

    // Then: expect 'b' to be unused
    subject.logAllWarnings(buf::append);
    assertEquals("Unexpected config parameter: 'foo.b:false' in 'Test'", buf.toString());
  }

  @Test
  void unknownParameters() {
    // Given: two parameters a and b
    var subject = newNodeAdapterForTest("{ foo : { a: true, b: false } }");

    // When: Access ONLY parameter 'a', but not 'b'
    subject.of("foo").asObject().of("a").asBoolean();

    assertTrue(subject.hasUnknownParameters());
  }

  @Test
  void allParametersAreKnown() {
    // Given: two parameters a and b
    var subject = newNodeAdapterForTest("{ foo : { a: true, b: false } }");

    var object = subject.of("foo").asObject();
    object.of("a").asBoolean();
    object.of("b").asBoolean();

    assertFalse(subject.hasUnknownParameters());
  }

  private static String unusedParams(NodeAdapter subject) {
    var buf = new StringBuilder();
    subject.logAllWarnings(m -> buf.append('\n').append(m));
    return buf.isEmpty() ? NON_UNUSED_PARAMETERS : buf.substring(1);
  }

  private enum AnEnum {
    A,
    B,
    A_B_C,
  }

  private record ARecord(String a) {
    static ARecord fromJson(NodeAdapter c) {
      return new ARecord(c.of("a").since(V2_4).summary("Summary A").asString());
    }
  }
}
