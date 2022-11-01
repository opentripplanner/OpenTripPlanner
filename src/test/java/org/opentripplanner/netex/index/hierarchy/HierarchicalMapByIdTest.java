package org.opentripplanner.netex.index.hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.netex.index.hierarchy.E.EASTWOOD;
import static org.opentripplanner.netex.index.hierarchy.E.SCHWARZENEGGER;
import static org.opentripplanner.netex.index.hierarchy.SetSupport.sort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * This test extends the tests performed in {@link HierarchicalMapTest} with respect to the few
 * extra features that is added in the {@link HierarchicalMapById} class.
 */
public class HierarchicalMapByIdTest {

  private final HierarchicalMapById<E> root = new HierarchicalMapById<>();
  private final HierarchicalMapById<E> child = new HierarchicalMapById<>(root);

  @Test
  public void testAddAllAndLocalMethods() {
    // Given
    HierarchicalMapById<E> subject = new HierarchicalMapById<>();
    String eId = EASTWOOD.getId();
    String sId = SCHWARZENEGGER.getId();

    // When
    subject.addAll(List.of(EASTWOOD, SCHWARZENEGGER));

    // Then
    assertEquals(Set.of(eId, sId), subject.localKeys());
    assertEquals(EASTWOOD, subject.localGet(eId));
    assertEquals(sort(List.of(EASTWOOD, SCHWARZENEGGER)), sort(subject.localValues()));
    assertTrue(subject.localContainsKey(eId));
  }

  /**
   * Add entity using the one argument add method verify that it can be retrieved using its id from
   * the map.
   */
  @Test
  public void addEntityWithOneArgument() {
    // Given one element added
    child.add(EASTWOOD);
    // The expect that element to be retrieved by its id
    assertEquals(EASTWOOD, child.lookup(EASTWOOD.getId()));
  }

  @Test
  public void addEntityWithIllegalKeyArgument() {
    // Prevent using add method with (key, value) -> the key is given: the ID.
    assertThrows(IllegalArgumentException.class, () -> child.add("Illegal key", EASTWOOD));
  }

  @Test
  public void addEntitiesWithIllegalKeyArgument() {
    Map<String, E> input = new HashMap<>();
    input.put("A", EASTWOOD);

    // Prevent using add method with (key, value) -> the key is given: the ID.
    assertThrows(IllegalArgumentException.class, () -> child.addAll(input));
  }

  @Test
  public void addToRootAndRetrieveFromChild() {
    root.add(SCHWARZENEGGER);
    assertEquals(SCHWARZENEGGER, child.lookup(SCHWARZENEGGER.getId()));
  }

  @Test
  public void testToStringAndSize() {
    assertEquals("size = 0", root.toString());
    assertEquals("size = 0", child.toString());
    root.add(SCHWARZENEGGER);
    assertEquals("size = 1", root.toString());
    assertEquals("size = 1", child.toString());
    child.add(EASTWOOD);
    assertEquals("size = 1", root.toString());
    assertEquals("size = 2", child.toString());
  }
}
