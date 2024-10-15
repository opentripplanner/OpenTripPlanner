package org.opentripplanner.netex.index.hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.netex.index.hierarchy.E.EASTWOOD;
import static org.opentripplanner.netex.index.hierarchy.E.REAGAN;
import static org.opentripplanner.netex.index.hierarchy.E.SCHWARZENEGGER;
import static org.opentripplanner.netex.index.hierarchy.SetSupport.sort;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * We uses a simplified version of the data structure used in the {@link HierarchicalMapTest}:
 * <pre>
 * Level\Role | President | Governor         | Actor
 * -----------|-----------|------------------|---------------
 *    Country | [Reagan]  |                  | Eastwood
 *      State |           | [Schwarzenegger] | Schwarzenegger
 * </pre>
 * Now we use this test setup to test the {@link HierarchicalMultimap}.
 */
public class HierarchicalMultimapTest {

  private static final String PRESIDENT = "president";
  private static final String GOVERNOR = "governor";
  private static final String ACTOR = "actor";
  private static final List<E> PRESIDENTS = Collections.singletonList(REAGAN);
  private static final List<E> ACTORS_COUNTRY = Collections.singletonList(EASTWOOD);
  private static final List<E> ACTORS_STATE = Collections.singletonList(SCHWARZENEGGER);
  private final HierarchicalMultimap<String, E> country = new HierarchicalMultimap<>();
  private final HierarchicalMultimap<String, E> state = new HierarchicalMultimap<>(country);

  @BeforeEach
  public void setup() {
    country.add(PRESIDENT, REAGAN);
    state.add(GOVERNOR, SCHWARZENEGGER);

    country.add(ACTOR, EASTWOOD);
    state.add(ACTOR, SCHWARZENEGGER);
  }

  @Test
  public void testAddAllAndLocalMethods() {
    // Given
    HierarchicalMultimap<String, E> subject = new HierarchicalMultimap<>();
    Multimap<String, E> input = ArrayListMultimap.create();

    input.put("A", EASTWOOD);
    input.put("A", SCHWARZENEGGER);
    input.put("B", REAGAN);

    // When
    subject.addAll(input);

    // Then
    assertEquals(Set.of("A", "B"), subject.localKeys());
    assertEquals(sort(List.of(EASTWOOD, SCHWARZENEGGER)), sort(subject.localGet("A")));
    assertEquals(sort(List.of(REAGAN)), sort(subject.localGet("B")));
    assertTrue(subject.localContainsKey("A"));
  }

  @Test
  public void addAndLookup() {
    // Given added elements in the setup method

    // Then expect Reagen to be president at all levels
    assertEquals(PRESIDENTS, country.lookup(PRESIDENT));
    assertEquals(PRESIDENTS, state.lookup(PRESIDENT));

    // And then the right actor for each level
    assertEquals(ACTORS_COUNTRY, country.lookup(ACTOR));
    assertEquals(ACTORS_STATE, state.lookup(ACTOR));

    // And no governor at country level; Hence an empty collection is returned
    assertTrue(country.lookup(GOVERNOR).isEmpty());
  }

  @Test
  public void addAll() {
    ArrayListMultimap<String, E> map = ArrayListMultimap.create();
    map.put(ACTOR, SCHWARZENEGGER);
    map.put(ACTOR, EASTWOOD);
    country.addAll(map);

    for (Collection<E> value : country.localValues()) {
      // 3 Actors
      if (value.size() == 3) {
        // Expect Eastwood twice, since the value is a list, not a set.
        assertEquals("[E(Eastwood, 1), E(Schwarzenegger, 1), E(Eastwood, 1)]", value.toString());
      }
      // 1 precident
      else if (value.size() == 1) {
        assertEquals("[E(Reagan, 1)]", value.toString());
      } else {
        fail("Unexpected: " + value);
      }
    }
  }

  @Test
  public void localKeys() {
    assertEquals(Set.of(PRESIDENT, ACTOR), country.localKeys());
    assertEquals(Set.of(GOVERNOR, ACTOR), state.localKeys());
  }

  @Test
  public void testToStringAndSize() {
    assertEquals("size = 2", country.toString());
    assertEquals("size = 4", state.toString());
  }

  @Test
  public void localRemove() {
    country.localRemove(PRESIDENT);
    assertEquals(1, country.localSize());
  }
}
