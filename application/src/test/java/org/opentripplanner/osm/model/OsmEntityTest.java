package org.opentripplanner.osm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.osm.model.TraverseDirection.BACKWARD;
import static org.opentripplanner.osm.model.TraverseDirection.DIRECTIONLESS;
import static org.opentripplanner.osm.model.TraverseDirection.FORWARD;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;

public class OsmEntityTest {

  @Test
  void testHasTag() {
    var o = new OsmTestEntity();
    assertFalse(o.hasTag("foo"));
    assertFalse(o.hasTag("FOO"));

    o = new OsmTestEntity("foo", "bar");
    assertTrue(o.hasTag("foo"));
    assertTrue(o.hasTag("FOO"));
  }

  @Test
  void testGetTag() {
    var o = new OsmTestEntity();
    assertNull(o.getTag("foo"));
    assertNull(o.getTag("FOO"));

    o = new OsmTestEntity("foo", "bar");
    assertEquals("bar", o.getTag("foo"));
    assertEquals("bar", o.getTag("FOO"));
  }

  @Test
  void testIsFalse() {
    assertTrue(OsmEntity.isFalse("no"));
    assertTrue(OsmEntity.isFalse("0"));
    assertTrue(OsmEntity.isFalse("false"));

    assertFalse(OsmEntity.isFalse("yes"));
    assertFalse(OsmEntity.isFalse("1"));
    assertFalse(OsmEntity.isFalse("true"));
    assertFalse(OsmEntity.isFalse("foo"));
    assertFalse(OsmEntity.isFalse("bar"));
    assertFalse(OsmEntity.isFalse("baz"));
  }

  @Test
  void testIsTrue() {
    assertTrue(OsmEntity.isTrue("yes"));
    assertTrue(OsmEntity.isTrue("1"));
    assertTrue(OsmEntity.isTrue("true"));

    assertFalse(OsmEntity.isTrue("no"));
    assertFalse(OsmEntity.isTrue("0"));
    assertFalse(OsmEntity.isTrue("false"));
    assertFalse(OsmEntity.isTrue("foo"));
    assertFalse(OsmEntity.isTrue("bar"));
    assertFalse(OsmEntity.isTrue("baz"));
  }

  @Test
  void testIsTagFalseOrTrue() {
    var o = new OsmTestEntity();
    assertFalse(o.isTagFalse("foo"));
    assertFalse(o.isTagFalse("FOO"));
    assertFalse(o.isTagTrue("foo"));
    assertFalse(o.isTagTrue("FOO"));

    o = new OsmTestEntity("foo", "true");
    assertFalse(o.isTagFalse("foo"));
    assertFalse(o.isTagFalse("FOO"));
    assertTrue(o.isTagTrue("foo"));
    assertTrue(o.isTagTrue("FOO"));

    o = new OsmTestEntity("foo", "no");
    assertTrue(o.isTagFalse("foo"));
    assertTrue(o.isTagFalse("FOO"));
    assertFalse(o.isTagTrue("foo"));
    assertFalse(o.isTagTrue("FOO"));
  }

  @Test
  void isTag() {
    var name = "Brendan";
    var osm = new OsmTestEntity("name", name);

    assertTrue(osm.isTag("name", name));
    assertTrue(osm.isTag("NAME", name));
    assertFalse(osm.isTag("NAMEE", name));
  }

  @Test
  void testDoesAllowTagAccess() {
    var o = new OsmTestEntity();
    assertFalse(o.isExplicitlyAllowed("foo"));

    o = new OsmTestEntity("foo", "bar");
    assertFalse(o.isExplicitlyAllowed("foo"));

    o = new OsmTestEntity("foo", "designated");
    assertTrue(o.isExplicitlyAllowed("foo"));

    o = new OsmTestEntity("foo", "official");
    assertTrue(o.isExplicitlyAllowed("foo"));
  }

  @Test
  void testIsGeneralAccessDenied() {
    var o = new OsmTestEntity();
    assertFalse(o.isGeneralAccessDenied());

    o = new OsmTestEntity("access", "something");
    assertFalse(o.isGeneralAccessDenied());

    o = new OsmTestEntity("access", "license");
    assertTrue(o.isGeneralAccessDenied());

    o = new OsmTestEntity("access", "no");
    assertTrue(o.isGeneralAccessDenied());
  }

  @Test
  void testIsDirectionalGeneralAccessDenied() {
    var o = new OsmTestEntity(Map.of("access", "yes", "access:backward", "no"));
    assertFalse(o.isGeneralAccessDenied(DIRECTIONLESS));
    assertFalse(o.isGeneralAccessDenied());
    assertTrue(o.isGeneralAccessDenied(BACKWARD));
    assertFalse(o.isGeneralAccessDenied(FORWARD));

    var p = new OsmTestEntity(Map.of("access", "no", "access:backward", "yes"));
    assertTrue(p.isGeneralAccessDenied(DIRECTIONLESS));
    assertTrue(p.isGeneralAccessDenied());
    assertFalse(p.isGeneralAccessDenied(BACKWARD));
    assertTrue(p.isGeneralAccessDenied(FORWARD));
  }

  @Test
  void testBicycleDenied() {
    var tags = new OsmTestEntity();
    assertFalse(tags.isBicycleDenied());

    for (var allowedValue : List.of("yes", "unknown", "somevalue")) {
      tags = new OsmTestEntity("bicycle", allowedValue);
      assertFalse(tags.isBicycleDenied(), "bicycle=" + allowedValue);
    }

    for (var deniedValue : List.of("no", "dismount", "license")) {
      tags = new OsmTestEntity("bicycle", deniedValue);
      assertTrue(tags.isBicycleDenied(), "bicycle=" + deniedValue);
    }
  }

  @Test
  void testBicycleDeniedOnVehicleDenied() {
    var noVehicle = new OsmTestEntity("vehicle", "no");
    assertTrue(noVehicle.isBicycleDenied());
    noVehicle = new OsmTestEntity(Map.of("vehicle", "no", "bicycle", "yes"));
    assertFalse(noVehicle.isBicycleDenied());
  }

  @Test
  void getReferenceTags() {
    var osm = new OsmTestEntity("ref", "A");

    assertEquals(Set.of("A"), osm.getMultiTagValues(Set.of("ref", "test")));
    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("test")));
  }

  @Test
  void getEmptyRefList() {
    var osm = new OsmTestEntity("ref", "A");

    assertEquals(Set.of(), osm.getMultiTagValues(Set.of()));
  }

  @Test
  void ignoreRefCase() {
    var osm = new OsmTestEntity("ref:ifopt", "A");

    assertEquals(Set.of("A"), osm.getMultiTagValues(Set.of("ref:ifopt")));
  }

  @Test
  void readSemicolonSeparated() {
    var osm = new OsmTestEntity("ref:a", "A;A;B");

    assertEquals(Set.of("A", "B"), osm.getMultiTagValues(Set.of("ref:A")));
  }

  @Test
  void removeBlankRef() {
    var osm = new OsmTestEntity(Map.of("ref1", " ", "ref2", ""));

    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("ref1")));
    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("ref2")));
  }

  @Test
  void shouldNotReturnNull() {
    var osm = new OsmTestEntity(Map.of("ref1", " ", "ref2", ""));

    assertEquals(Set.of(), osm.getMultiTagValues(Set.of()));
    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("ref3")));
  }

  @Test
  void isWheelchairAccessible() {
    var osm1 = new OsmTestEntity();
    assertTrue(osm1.isWheelchairAccessible());

    var osm2 = new OsmTestEntity("wheelchair", "no");
    assertFalse(osm2.isWheelchairAccessible());

    var osm3 = new OsmTestEntity("wheelchair", "yes");
    assertTrue(osm3.isWheelchairAccessible());
  }

  private static Stream<Arguments> barrierWheelchairAccessibilityCases() {
    return Stream.of(
      Arguments.of(OsmNode.of().withTag("barrier", "stile").build(), false),
      Arguments.of(
        OsmNode.of().withTag("barrier", "stile").withTag("wheelchair", "yes").build(),
        true
      ),
      Arguments.of(OsmNode.of().withTag("barrier", "kerb").build(), false),
      // https://wiki.openstreetmap.org/wiki/Key:kerb
      Arguments.of(OsmNode.of().withTag("barrier", "kerb").withTag("kerb", "flush").build(), true),
      Arguments.of(
        OsmNode.of().withTag("barrier", "kerb").withTag("kerb", "lowered").build(),
        true
      ),
      Arguments.of(OsmNode.of().withTag("barrier", "kerb").withTag("kerb", "no").build(), true),
      Arguments.of(
        OsmNode.of().withTag("barrier", "kerb").withTag("kerb", "raised").build(),
        false
      ),
      Arguments.of(
        OsmNode.of().withTag("barrier", "kerb").withTag("kerb", "rolled").build(),
        false
      ),
      Arguments.of(OsmNode.of().withTag("barrier", "kerb").withTag("kerb", "yes").build(), false)
    );
  }

  @ParameterizedTest
  @MethodSource("barrierWheelchairAccessibilityCases")
  void isBarrierWheelchairAccessible(OsmEntity osm, boolean expected) {
    assertEquals(expected, osm.isWheelchairAccessible());
  }

  @Test
  void wheelchairAccessibility() {
    var osm1 = new OsmTestEntity();
    assertEquals(Accessibility.NO_INFORMATION, osm1.explicitWheelchairAccessibility());

    var osm2 = new OsmTestEntity("wheelchair", "no");
    assertEquals(Accessibility.NOT_POSSIBLE, osm2.explicitWheelchairAccessibility());

    var osm3 = new OsmTestEntity("wheelchair", "yes");
    assertEquals(Accessibility.POSSIBLE, osm3.explicitWheelchairAccessibility());
  }

  @Test
  void isRoutable() {
    assertFalse(WayTestData.zooPlatform().isRoutable());
    assertTrue(WayTestData.indoor("area").isRoutable());
    assertFalse(WayTestData.indoor("room").isRoutable());

    var highway = WayTestData.highwayWithCycleLane();
    assertTrue(highway.isRoutable());
    var modifiedHighway = highway.copy().withTag("access", "no").build();
    assertFalse(modifiedHighway.isRoutable());
    var furtherModified = modifiedHighway.copy().withTag("bicycle", "yes").build();
    assertTrue(furtherModified.isRoutable());
  }

  @Test
  void isPlatform() {
    assertFalse(WayTestData.zooPlatform().isPlatform());
  }

  @Test
  void platformEdge() {
    var w = new OsmTestEntity("railway", "platform_edge");
    assertTrue(w.isRoutable());
    assertTrue(w.isBoardingLocation());
    assertTrue(w.isPlatform());
  }

  @Test
  void testGenerateI18NForPattern() {
    var osmTags = new OsmTestEntity(
      Map.of(
        "note",
        "Note EN",
        "description:fr",
        "Description FR",
        "wheelchair:description",
        "Wheelchair description EN",
        "wheelchair:description:fr",
        "Wheelchair description FR"
      )
    );

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
    var nameless = WayTestData.highwayWithCycleLane();
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
    var key = "capacity:disabled";
    var way = new OsmTestEntity(key, value);
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
    var key = "duration";
    var way = new OsmTestEntity(key, value);
    var duration = way.getTagValueAsDuration(key, i -> {});
    assertEquals(expected, duration);
  }
}
