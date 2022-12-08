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

public class OSMWithTagsTest {

  @Test
  public void testHasTag() {
    OSMWithTags o = new OSMWithTags();
    assertFalse(o.hasTag("foo"));
    assertFalse(o.hasTag("FOO"));
    o.addTag("foo", "bar");

    assertTrue(o.hasTag("foo"));
    assertTrue(o.hasTag("FOO"));
  }

  @Test
  public void testGetTag() {
    OSMWithTags o = new OSMWithTags();
    assertNull(o.getTag("foo"));
    assertNull(o.getTag("FOO"));

    o.addTag("foo", "bar");
    assertEquals("bar", o.getTag("foo"));
    assertEquals("bar", o.getTag("FOO"));
  }

  @Test
  public void testIsFalse() {
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
  public void testIsTrue() {
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
  public void testIsTagFalseOrTrue() {
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
  public void testDoesAllowTagAccess() {
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
  public void testIsGeneralAccessDenied() {
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
  public void testBicycleDenied() {
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
  public void getReferenceTags() {
    var osm = new OSMWithTags();
    osm.addTag("ref", "A");

    assertEquals(Set.of("A"), osm.getMultiTagValues(Set.of("ref", "test")));
    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("test")));
  }

  @Test
  public void getEmptyRefList() {
    var osm = new OSMWithTags();
    osm.addTag("ref", "A");

    assertEquals(Set.of(), osm.getMultiTagValues(Set.of()));
  }

  @Test
  public void ignoreRefCase() {
    var osm = new OSMWithTags();
    osm.addTag("ref:IFOPT", "A");

    assertEquals(Set.of("A"), osm.getMultiTagValues(Set.of("ref:ifopt")));
  }

  @Test
  public void readSemicolonSeparated() {
    var osm = new OSMWithTags();
    osm.addTag("ref:A", "A;A;B");

    assertEquals(Set.of("A", "B"), osm.getMultiTagValues(Set.of("ref:A")));
  }

  @Test
  public void removeBlankRef() {
    var osm = new OSMWithTags();
    osm.addTag("ref1", " ");
    osm.addTag("ref2", "");

    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("ref1")));
    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("ref2")));
  }

  @Test
  public void shouldNotReturnNull() {
    var osm = new OSMWithTags();
    osm.addTag("ref1", " ");
    osm.addTag("ref2", "");

    assertEquals(Set.of(), osm.getMultiTagValues(Set.of()));
    assertEquals(Set.of(), osm.getMultiTagValues(Set.of("ref3")));
  }

  @Test
  public void testGenerateI18NForPattern() {
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
