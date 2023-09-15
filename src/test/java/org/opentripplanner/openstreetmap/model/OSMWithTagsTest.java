package org.opentripplanner.openstreetmap.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData;

public class OSMWithTagsTest {

  @Test
  void testHasTag() {
    OSMWithTags o = new OSMWithTags();
    assertFalse(o.hasTag("foo"));
    assertFalse(o.hasTag("FOO"));
    o.addTag("foo", "bar");

    assertTrue(o.hasTag("foo"));
    assertTrue(o.hasTag("FOO"));
  }

  @Test
  void testGetTag() {
    OSMWithTags o = new OSMWithTags();
    assertNull(o.getTag("foo"));
    assertNull(o.getTag("FOO"));

    o.addTag("foo", "bar");
    assertEquals("bar", o.getTag("foo"));
    assertEquals("bar", o.getTag("FOO"));
  }

  @Test
  void testIsFalse() {
    assertTrue(OSMWithTags.isFalse("no"));
    assertTrue(OSMWithTags.isFalse("0"));
    assertTrue(OSMWithTags.isFalse("false"));

    assertFalse(OSMWithTags.isFalse("yes"));
    assertFalse(OSMWithTags.isFalse("1"));
    assertFalse(OSMWithTags.isFalse("true"));
    assertFalse(OSMWithTags.isFalse("foo"));
    assertFalse(OSMWithTags.isFalse("bar"));
    assertFalse(OSMWithTags.isFalse("baz"));
  }

  @Test
  void testIsTrue() {
    assertTrue(OSMWithTags.isTrue("yes"));
    assertTrue(OSMWithTags.isTrue("1"));
    assertTrue(OSMWithTags.isTrue("true"));

    assertFalse(OSMWithTags.isTrue("no"));
    assertFalse(OSMWithTags.isTrue("0"));
    assertFalse(OSMWithTags.isTrue("false"));
    assertFalse(OSMWithTags.isTrue("foo"));
    assertFalse(OSMWithTags.isTrue("bar"));
    assertFalse(OSMWithTags.isTrue("baz"));
  }

  @Test
  void testIsTagFalseOrTrue() {
    OSMWithTags o = new OSMWithTags();
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
    var osm = new OSMWithTags();
    osm.addTag("NAME", name);

    assertTrue(osm.isTag("name", name));
    assertTrue(osm.isTag("NAME", name));
    assertFalse(osm.isTag("NAMEE", name));
  }

  @Test
  void testDoesAllowTagAccess() {
    OSMWithTags o = new OSMWithTags();
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
    OSMWithTags o = new OSMWithTags();
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
    OSMWithTags tags = new OSMWithTags();
    assertFalse(tags.isBicycleExplicitlyDenied());

    for (var allowedValue : List.of("yes", "unknown", "somevalue")) {
      tags.addTag("bicycle", allowedValue);
      assertFalse(tags.isBicycleExplicitlyDenied(), "bicycle=" + allowedValue);
    }

    for (var deniedValue : List.of("no", "dismount", "license", "use_sidepath")) {
      tags.addTag("bicycle", deniedValue);
      assertTrue(tags.isBicycleExplicitlyDenied(), "bicycle=" + deniedValue);
    }
  }

  @Test
  void getReferenceTags() {
    var osm = new OSMWithTags();
    osm.addTag("ref", "A");

    assertEquals(Set.of("A"), osm.getMultiTagValues(Set.of("ref", "test")));
    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("test")));
  }

  @Test
  void getEmptyRefList() {
    var osm = new OSMWithTags();
    osm.addTag("ref", "A");

    assertEquals(Set.of(), osm.getMultiTagValues(Set.of()));
  }

  @Test
  void ignoreRefCase() {
    var osm = new OSMWithTags();
    osm.addTag("ref:IFOPT", "A");

    assertEquals(Set.of("A"), osm.getMultiTagValues(Set.of("ref:ifopt")));
  }

  @Test
  void readSemicolonSeparated() {
    var osm = new OSMWithTags();
    osm.addTag("ref:A", "A;A;B");

    assertEquals(Set.of("A", "B"), osm.getMultiTagValues(Set.of("ref:A")));
  }

  @Test
  void removeBlankRef() {
    var osm = new OSMWithTags();
    osm.addTag("ref1", " ");
    osm.addTag("ref2", "");

    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("ref1")));
    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("ref2")));
  }

  @Test
  void shouldNotReturnNull() {
    var osm = new OSMWithTags();
    osm.addTag("ref1", " ");
    osm.addTag("ref2", "");

    assertEquals(Set.of(), osm.getMultiTagValues(Set.of()));
    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("ref3")));
  }

  @Test
  void isWheelchairAccessible() {
    var osm1 = new OSMWithTags();
    assertTrue(osm1.isWheelchairAccessible());

    var osm2 = new OSMWithTags();
    osm2.addTag("wheelchair", "no");
    assertFalse(osm2.isWheelchairAccessible());

    var osm3 = new OSMWithTags();
    osm3.addTag("wheelchair", "yes");
    assertTrue(osm3.isWheelchairAccessible());
  }

  @Test
  void isRoutable() {
    assertFalse(WayTestData.zooPlatform().isRoutable());
  }

  @Test
  void isPlatform() {
    assertFalse(WayTestData.zooPlatform().isPlatform());
  }

  @Test
  void testGenerateI18NForPattern() {
    OSMWithTags osmTags = new OSMWithTags();
    osmTags.addTag("note", "Note EN");
    osmTags.addTag("description:fr", "Description FR");
    osmTags.addTag("wheelchair:description", "Wheelchair description EN");
    osmTags.addTag("wheelchair:description:fr", "Wheelchair description FR");

    assertNull(osmTags.generateI18NForPattern(null));
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
}
