package org.opentripplanner.osm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;
import org.opentripplanner.transit.model.basic.Accessibility;

public class OsmWithTagsTest {

  @Test
  void testHasTag() {
    OsmWithTags o = new OsmWithTags();
    assertFalse(o.hasTag("foo"));
    assertFalse(o.hasTag("FOO"));
    o.addTag("foo", "bar");

    assertTrue(o.hasTag("foo"));
    assertTrue(o.hasTag("FOO"));
  }

  @Test
  void testGetTag() {
    OsmWithTags o = new OsmWithTags();
    assertNull(o.getTag("foo"));
    assertNull(o.getTag("FOO"));

    o.addTag("foo", "bar");
    assertEquals("bar", o.getTag("foo"));
    assertEquals("bar", o.getTag("FOO"));
  }

  @Test
  void testIsFalse() {
    assertTrue(OsmWithTags.isFalse("no"));
    assertTrue(OsmWithTags.isFalse("0"));
    assertTrue(OsmWithTags.isFalse("false"));

    assertFalse(OsmWithTags.isFalse("yes"));
    assertFalse(OsmWithTags.isFalse("1"));
    assertFalse(OsmWithTags.isFalse("true"));
    assertFalse(OsmWithTags.isFalse("foo"));
    assertFalse(OsmWithTags.isFalse("bar"));
    assertFalse(OsmWithTags.isFalse("baz"));
  }

  @Test
  void testIsTrue() {
    assertTrue(OsmWithTags.isTrue("yes"));
    assertTrue(OsmWithTags.isTrue("1"));
    assertTrue(OsmWithTags.isTrue("true"));

    assertFalse(OsmWithTags.isTrue("no"));
    assertFalse(OsmWithTags.isTrue("0"));
    assertFalse(OsmWithTags.isTrue("false"));
    assertFalse(OsmWithTags.isTrue("foo"));
    assertFalse(OsmWithTags.isTrue("bar"));
    assertFalse(OsmWithTags.isTrue("baz"));
  }

  @Test
  void testIsTagFalseOrTrue() {
    OsmWithTags o = new OsmWithTags();
    assertFalse(o.isTagFalse("foo"));
    assertFalse(o.isTagFalse("FOO"));
    assertFalse(o.isTagTrue("foo"));
    assertFalse(o.isTagTrue("FOO"));

    o.addTag("foo", "true");
    assertFalse(o.isTagFalse("foo"));
    assertFalse(o.isTagFalse("FOO"));
    assertTrue(o.isTagTrue("foo"));
    assertTrue(o.isTagTrue("FOO"));

    o.addTag("foo", "no");
    assertTrue(o.isTagFalse("foo"));
    assertTrue(o.isTagFalse("FOO"));
    assertFalse(o.isTagTrue("foo"));
    assertFalse(o.isTagTrue("FOO"));
  }

  @Test
  void isTag() {
    var name = "Brendan";
    var osm = new OsmWithTags();
    osm.addTag("NAME", name);

    assertTrue(osm.isTag("name", name));
    assertTrue(osm.isTag("NAME", name));
    assertFalse(osm.isTag("NAMEE", name));
  }

  @Test
  void testDoesAllowTagAccess() {
    OsmWithTags o = new OsmWithTags();
    assertFalse(o.doesTagAllowAccess("foo"));

    o.addTag("foo", "bar");
    assertFalse(o.doesTagAllowAccess("foo"));

    o.addTag("foo", "designated");
    assertTrue(o.doesTagAllowAccess("foo"));

    o.addTag("foo", "official");
    assertTrue(o.doesTagAllowAccess("foo"));
  }

  @Test
  void testIsGeneralAccessDenied() {
    OsmWithTags o = new OsmWithTags();
    assertFalse(o.isGeneralAccessDenied());

    o.addTag("access", "something");
    assertFalse(o.isGeneralAccessDenied());

    o.addTag("access", "license");
    assertTrue(o.isGeneralAccessDenied());

    o.addTag("access", "no");
    assertTrue(o.isGeneralAccessDenied());
  }

  @Test
  void testBicycleDenied() {
    OsmWithTags tags = new OsmWithTags();
    assertFalse(tags.isBicycleExplicitlyDenied());

    for (var allowedValue : List.of("yes", "unknown", "somevalue")) {
      tags.addTag("bicycle", allowedValue);
      assertFalse(tags.isBicycleExplicitlyDenied(), "bicycle=" + allowedValue);
    }

    for (var deniedValue : List.of("no", "dismount", "license")) {
      tags.addTag("bicycle", deniedValue);
      assertTrue(tags.isBicycleExplicitlyDenied(), "bicycle=" + deniedValue);
    }
  }

  @Test
  void getReferenceTags() {
    var osm = new OsmWithTags();
    osm.addTag("ref", "A");

    assertEquals(Set.of("A"), osm.getMultiTagValues(Set.of("ref", "test")));
    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("test")));
  }

  @Test
  void getEmptyRefList() {
    var osm = new OsmWithTags();
    osm.addTag("ref", "A");

    assertEquals(Set.of(), osm.getMultiTagValues(Set.of()));
  }

  @Test
  void ignoreRefCase() {
    var osm = new OsmWithTags();
    osm.addTag("ref:IFOPT", "A");

    assertEquals(Set.of("A"), osm.getMultiTagValues(Set.of("ref:ifopt")));
  }

  @Test
  void readSemicolonSeparated() {
    var osm = new OsmWithTags();
    osm.addTag("ref:A", "A;A;B");

    assertEquals(Set.of("A", "B"), osm.getMultiTagValues(Set.of("ref:A")));
  }

  @Test
  void removeBlankRef() {
    var osm = new OsmWithTags();
    osm.addTag("ref1", " ");
    osm.addTag("ref2", "");

    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("ref1")));
    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("ref2")));
  }

  @Test
  void shouldNotReturnNull() {
    var osm = new OsmWithTags();
    osm.addTag("ref1", " ");
    osm.addTag("ref2", "");

    assertEquals(Set.of(), osm.getMultiTagValues(Set.of()));
    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("ref3")));
  }

  @Test
  void isWheelchairAccessible() {
    var osm1 = new OsmWithTags();
    assertTrue(osm1.isWheelchairAccessible());

    var osm2 = new OsmWithTags();
    osm2.addTag("wheelchair", "no");
    assertFalse(osm2.isWheelchairAccessible());

    var osm3 = new OsmWithTags();
    osm3.addTag("wheelchair", "yes");
    assertTrue(osm3.isWheelchairAccessible());
  }

  @Test
  void wheelchairAccessibility() {
    var osm1 = new OsmWithTags();
    assertEquals(Accessibility.NO_INFORMATION, osm1.wheelchairAccessibility());

    var osm2 = new OsmWithTags();
    osm2.addTag("wheelchair", "no");
    assertEquals(Accessibility.NOT_POSSIBLE, osm2.wheelchairAccessibility());

    var osm3 = new OsmWithTags();
    osm3.addTag("wheelchair", "yes");
    assertEquals(Accessibility.POSSIBLE, osm3.wheelchairAccessibility());
  }

  @Test
  void isRoutable() {
    assertFalse(WayTestData.zooPlatform().isRoutable());
    assertTrue(WayTestData.indoor("area").isRoutable());
    assertFalse(WayTestData.indoor("room").isRoutable());
  }

  @Test
  void isPlatform() {
    assertFalse(WayTestData.zooPlatform().isPlatform());
  }

  @Test
  void testGenerateI18NForPattern() {
    OsmWithTags osmTags = new OsmWithTags();
    osmTags.addTag("note", "Note EN");
    osmTags.addTag("description:fr", "Description FR");
    osmTags.addTag("wheelchair:description", "Wheelchair description EN");
    osmTags.addTag("wheelchair:description:fr", "Wheelchair description FR");

    Map<String, String> expected = new HashMap<>();

    expected.put(null, "");
    assertEquals(expected, osmTags.generateI18NForPattern(""));

    expected.clear();
    expected.put(null, "Static text");
    assertEquals(expected, osmTags.generateI18NForPattern("Static text"));

    expected.clear();
    expected.put(null, "Note: Note EN");
    assertEquals(expected, osmTags.generateI18NForPattern("Note: {note}"));

    expected.clear();
    expected.put(null, "Desc: Description FR");
    expected.put("fr", "Desc: Description FR");
    assertEquals(expected, osmTags.generateI18NForPattern("Desc: {description}"));

    expected.clear();
    expected.put(null, "Note: Note EN, Wheelchair description EN");
    expected.put("fr", "Note: Note EN, Wheelchair description FR");
    assertEquals(
      expected,
      osmTags.generateI18NForPattern("Note: {note}, {wheelchair:description}")
    );

    expected.clear();
    expected.put(null, "Note: Note EN, Wheelchair description EN, ");
    expected.put("fr", "Note: Note EN, Wheelchair description FR, ");
    assertEquals(
      expected,
      osmTags.generateI18NForPattern("Note: {note}, {wheelchair:description}, {foobar:description}")
    );
  }

  @Test
  void fallbackName() {
    var nameless = WayTestData.cycleway();
    assertTrue(nameless.hasNoName());

    var namedTunnel = WayTestData.carTunnel();
    assertFalse(namedTunnel.hasNoName());
  }

  private static List<Arguments> parseIntOrBooleanCases() {
    return List.of(
      Arguments.of("true", OptionalInt.of(1)),
      Arguments.of("yes", OptionalInt.of(1)),
      Arguments.of("no", OptionalInt.of(0)),
      Arguments.of("false", OptionalInt.of(0)),
      Arguments.of("0", OptionalInt.of(0)),
      Arguments.of("12", OptionalInt.of(12)),
      Arguments.of("", OptionalInt.empty())
    );
  }

  @ParameterizedTest
  @MethodSource("parseIntOrBooleanCases")
  void parseIntOrBoolean(String value, OptionalInt expected) {
    var way = new OsmWithTags();
    var key = "capacity:disabled";
    way.addTag(key, value);
    var maybeInt = way.parseIntOrBoolean(key, i -> {});
    assertEquals(expected, maybeInt);
  }

  private static List<Arguments> parseTagAsDurationCases() {
    return List.of(
      Arguments.of("00:11", Optional.of(Duration.ofMinutes(11))),
      Arguments.of("11", Optional.of(Duration.ofMinutes(11))),
      Arguments.of("1:22:33", Optional.of(Duration.ofHours(1).plusMinutes(22).plusSeconds(33))),
      Arguments.of("82", Optional.of(Duration.ofMinutes(82))),
      Arguments.of("25:00", Optional.of(Duration.ofHours(25))),
      Arguments.of("25:00:00", Optional.of(Duration.ofHours(25))),
      Arguments.of("22:60", Optional.empty()),
      Arguments.of("10:61:40", Optional.empty()),
      Arguments.of("10:59:60", Optional.empty()),
      Arguments.of("1:12:34", Optional.of(Duration.ofHours(1).plusMinutes(12).plusSeconds(34))),
      Arguments.of("1:2:34", Optional.empty()),
      Arguments.of("1:12:3", Optional.empty()),
      Arguments.of("1:2", Optional.empty())
    );
  }

  @ParameterizedTest
  @MethodSource("parseTagAsDurationCases")
  void parseTagAsDuration(String value, Optional<Duration> expected) {
    var way = new OsmWithTags();
    var key = "duration";
    way.addTag(key, value);
    var duration = way.getTagValueAsDuration(key, i -> {});
    assertEquals(expected, duration);
  }
}
