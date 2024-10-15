package org.opentripplanner.netex.index.hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.netex.index.hierarchy.E.EASTWOOD;
import static org.opentripplanner.netex.index.hierarchy.E.REAGAN;
import static org.opentripplanner.netex.index.hierarchy.E.SCHWARZENEGGER;
import static org.opentripplanner.netex.index.hierarchy.SetSupport.sort;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.index.api.HMapValidationRule;

/**
 * To test the hierarchical map we construct a hierarchy of:
 * <pre>
 *     Country(root) -> State -> City(leaf)
 * </pre>
 * We add <em>president</em> <b>Regan</b> to <b>country</b>, a <em>governor</em>
 * <b>Schwarzenegger</b> to <b>state</b> and <em>mayor</em> <b>Eastwood</b> to <b>City</b>.
 * <p>
 * We also add an <em>actor</em> for each level, but this time <b>Eastwood</b> is added to the
 * <b>country</b> level (clearly the best actor ;-). <b>Schwarzenegger</b> is
 * added to the <b>state</b> level, and <b>Reagan</b> to <b>City</b>.
 * <pre>
 *            |                        R o l e                         |
 * Level      | President | Governor       | Mayor    | Actor          |
 * -----------|-----------|----------------|----------|----------------|
 *  Country   | Reagan    |                |          | Eastwood       |
 *  State     |           | Schwarzenegger |          | Schwarzenegger |
 *  City      |           |                | Eastwood | Reagan         |
 * </pre>
 * Now we use this test setup to test the {@link HierarchicalMap} and {@link
 * AbstractHierarchicalMap} class.
 */
public class HierarchicalMapTest {

  private static final String PRESIDENT = "president";
  private static final String GOVERNOR = "governor";
  private static final String MAYOR = "mayor";
  private static final String ACTOR = "actor";

  private final HierarchicalMap<String, E> country = new HierarchicalMap<>();
  private final HierarchicalMap<String, E> state = new HierarchicalMap<>(country);
  private final HierarchicalMap<String, E> city = new HierarchicalMap<>(state);

  @BeforeEach
  public void setup() {
    country.add(PRESIDENT, REAGAN);
    state.add(GOVERNOR, SCHWARZENEGGER);
    city.add(MAYOR, EASTWOOD);

    country.add(ACTOR, EASTWOOD);
    state.add(ACTOR, SCHWARZENEGGER);
    city.add(ACTOR, REAGAN);
  }

  @Test
  public void addAndLookup() {
    // Given added elements in the setup method

    // Then expect Reagen to be president at all levels
    assertEquals(REAGAN, country.lookup(PRESIDENT));
    assertEquals(REAGAN, state.lookup(PRESIDENT));
    assertEquals(REAGAN, city.lookup(PRESIDENT));

    // And then the right actor for each level
    assertEquals(EASTWOOD, country.lookup(ACTOR));
    assertEquals(SCHWARZENEGGER, state.lookup(ACTOR));
    assertEquals(REAGAN, city.lookup(ACTOR));

    // And no governor at country and state level
    assertNull(country.lookup(MAYOR));
    assertNull(state.lookup(MAYOR));
  }

  @Test
  public void testAddAllAndLocalMethods() {
    // Given
    Map<String, E> map = new HashMap<>();
    map.put("A", EASTWOOD);
    map.put("B", SCHWARZENEGGER);

    // When
    HierarchicalMap<String, E> hmap = new HierarchicalMap<>();
    hmap.addAll(map);

    // Then
    assertEquals(Set.of("A", "B"), hmap.localKeys());
    assertEquals(EASTWOOD, hmap.localGet("A"));
    assertEquals(sort(List.of(EASTWOOD, SCHWARZENEGGER)), sort(hmap.localValues()));
    assertTrue(hmap.localContainsKey("B"));
  }

  @Test
  public void localValues() {
    // Given added elements in the setup method
    assertEqElements(List.of(EASTWOOD, REAGAN), country.localValues());
    assertEqElements(List.of(SCHWARZENEGGER, SCHWARZENEGGER), state.localValues());
    assertEqElements(List.of(EASTWOOD, REAGAN), city.localValues());
  }

  @Test
  public void localGet() {
    // Given added elements in the setup method

    // Then expect null when using a missing key at a given hierarchy level
    assertNull(country.localGet(MAYOR));
    assertNull(state.localGet(MAYOR));
    assertNull(state.localGet(PRESIDENT));
    assertNull(city.localGet(PRESIDENT));

    // Then expect to get the right object for the given hierarchy level
    assertEquals(EASTWOOD, country.localGet(ACTOR));
    assertEquals(SCHWARZENEGGER, state.localGet(GOVERNOR));
    assertEquals(REAGAN, city.localGet(ACTOR));
  }

  @Test
  public void containsKey() {
    // Given added elements in the setup method

    // At country level expect president present, not governor and mayor
    assertTrue(country.containsKey(PRESIDENT));
    assertFalse(country.containsKey(GOVERNOR));
    assertFalse(country.containsKey(MAYOR));

    // At state level expect president and governor to be present, not mayor
    assertTrue(state.containsKey(PRESIDENT));
    assertTrue(state.containsKey(GOVERNOR));
    assertFalse(state.containsKey(MAYOR));

    // At country level expect all keys to be present: president, governor and mayor
    assertTrue(city.containsKey(PRESIDENT));
    assertTrue(city.containsKey(GOVERNOR));
    assertTrue(city.containsKey(MAYOR));
  }

  @Test
  public void localContainsKey() {
    // Given added elements in the setup method

    // At country level expect president and actor to be present, not mayor
    assertTrue(country.localContainsKey(PRESIDENT));
    assertTrue(country.localContainsKey(ACTOR));
    assertFalse(country.localContainsKey(MAYOR));

    // At state level expect governor and actor to be present, not president
    assertTrue(state.localContainsKey(GOVERNOR));
    assertTrue(state.localContainsKey(ACTOR));
    assertFalse(state.localContainsKey(PRESIDENT));

    // At country level expect mayor and actor to be present, not governor
    assertTrue(city.localContainsKey(MAYOR));
    assertTrue(city.localContainsKey(ACTOR));
    assertFalse(city.localContainsKey(GOVERNOR));
  }

  /** Test localSize(), size() and toString(): "size = N" */
  @Test
  public void sizeAndToString() {
    // Given added elements in the setup method
    assertEquals(2, country.localSize());
    assertEquals(2, country.size());
    assertEquals("size = 2", country.toString());

    assertEquals(2, state.localSize());
    assertEquals(4, state.size());
    assertEquals("size = 4", state.toString());

    assertEquals(2, city.localSize());
    assertEquals(4, city.size());
    assertEquals("size = 4", city.toString());
  }

  @Test
  public void localRemove() {
    int originalCitySize = city.size();

    country.localRemove(GOVERNOR);
    city.localRemove(GOVERNOR);
    // GOVERNOR belongs to state, not city or country - no change expected
    assertEquals(originalCitySize, city.size());

    // Remove GOVERNOR -> expect size to decrement by one
    state.localRemove(GOVERNOR);
    assertEquals(originalCitySize - 1, city.size());
    assertNull(state.localGet(GOVERNOR), "GOVERNOR is removed from office");
  }

  @Test
  public void validate() {
    // Given a filter to remove REAGAN
    HMapValidationRule<String, E> reagenFilter = new HMapValidationRule<>() {
      @Override
      public Status validate(E element) {
        return REAGAN.equals(element) ? Status.DISCARD : Status.OK;
      }

      @Override
      public DataImportIssue logMessage(String key, E value) {
        return () -> String.format("%s %s is removed", key, value.name());
      }
    };

    // And a warning consumer
    final StringBuilder warningConsumer = new StringBuilder();

    // Then remove from CITY - actor removed
    city.validate(reagenFilter, i -> warningConsumer.append(i.getMessage()));
    assertEquals("actor Reagan is removed", warningConsumer.toString());
    assertEquals("[E(Eastwood, 1)]", city.localValues().toString());

    // Then remove from STATE - nothing to remove
    warningConsumer.setLength(0);
    state.validate(reagenFilter, i -> warningConsumer.append(i.getMessage()));
    assertEquals("", warningConsumer.toString());
    assertEquals(2, state.localSize());

    // Then remove from COUNTRY - president removed
    warningConsumer.setLength(0);
    country.validate(reagenFilter, i -> warningConsumer.append(i.getMessage()));
    assertEquals("president Reagan is removed", warningConsumer.toString());
    assertEquals("[E(Eastwood, 1)]", country.localValues().toString());
  }

  private void assertEqElements(Collection<E> expected, Collection<E> actual) {
    assertEquals(sort(expected), sort(actual));
  }
}
