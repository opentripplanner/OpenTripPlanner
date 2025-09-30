package org.opentripplanner.netex.index.hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.netex.index.hierarchy.E.EASTWOOD;
import static org.opentripplanner.netex.index.hierarchy.E.REAGAN;
import static org.opentripplanner.netex.index.hierarchy.E.REAGAN_2;
import static org.opentripplanner.netex.index.hierarchy.E.REAGAN_3;
import static org.opentripplanner.netex.index.hierarchy.E.SCHWARZENEGGER;
import static org.opentripplanner.netex.index.hierarchy.SetSupport.sort;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class HierarchicalVersionMapByIdTest {

  private final HierarchicalVersionMapById<E> root = new HierarchicalVersionMapById<>();
  private final HierarchicalVersionMapById<E> child = new HierarchicalVersionMapById<>(root);

  @Test
  public void testAddAllAndLocalMethods() {
    // Given
    HierarchicalVersionMapById<E> subject = new HierarchicalVersionMapById<>();

    String eId = EASTWOOD.getId();
    String sId = SCHWARZENEGGER.getId();
    String rId = REAGAN.getId();

    // When
    subject.addAll(List.of(REAGAN, REAGAN_2, REAGAN_3, EASTWOOD, SCHWARZENEGGER));

    // Then
    assertEquals(Set.of(eId, sId, rId), subject.localKeys());
    assertEquals(List.of(EASTWOOD), subject.localGet(eId));
    assertEquals(sort(List.of(REAGAN, REAGAN_2, REAGAN_3)), sort(subject.localGet(rId)));
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

    // Then expect the element to be retrieved by its id
    assertEquals(Collections.singletonList(EASTWOOD), child.lookup(EASTWOOD.getId()));

    // And expect an empty collection if no element exist with such a key
    assertTrue(child.lookup(SCHWARZENEGGER.getId()).isEmpty());
  }

  @Test
  public void addEntityWithIllegalKeyArgument() {
    // Prevent using add method with (key, value) -> the key is given: the ID.
    assertThrows(IllegalArgumentException.class, () -> child.add("Illegal key", EASTWOOD));
  }

  @Test
  public void addEntitiesWithIllegalKeyArgument() {
    Multimap<String, E> input = ArrayListMultimap.create();
    input.put("Illegal key", EASTWOOD);

    // Prevent using add method with (key, value) -> the key is given: the ID.
    assertThrows(IllegalArgumentException.class, () -> child.addAll(input));
  }

  @Test
  public void addToRootAndRetrieveFromChild() {
    root.add(SCHWARZENEGGER);
    assertEquals(List.of(SCHWARZENEGGER), child.lookup(SCHWARZENEGGER.getId()));
  }

  @Test
  public void lookupVersionedElement() {
    // Given to versions of the same element added
    root.add(REAGAN);
    root.add(REAGAN_2);

    // When
    E latestVersion = child.lookupLastVersionById(REAGAN.getId());

    // Then
    assertEquals(REAGAN_2, latestVersion);
    assertNotEquals(REAGAN, latestVersion);

    // Also verify all Reagans are present in the collection
    assertEquals(sort(List.of(REAGAN, REAGAN_2)), sort(child.lookup(REAGAN.getId())));
  }

  @Test
  public void isLatestVersionWithNoElements() {
    // Any element is the latest version if the collection is empty
    assertTrue(root.isNewerOrSameVersionComparedWithExistingValues(REAGAN));
  }

  @Test
  public void isLatestVersionWithOneRootElement() {
    // Given one element in the root
    root.add(REAGAN_2);

    // Then expect an older element to return false
    assertFalse(root.isNewerOrSameVersionComparedWithExistingValues(REAGAN));

    // And expect the same element to be true (equals)
    assertTrue(root.isNewerOrSameVersionComparedWithExistingValues(REAGAN_2));

    // And expect a newer element (same id) to be true
    assertTrue(root.isNewerOrSameVersionComparedWithExistingValues(REAGAN_3));

    // And expect an element with none existing id (map key) to return true
    assertTrue(root.isNewerOrSameVersionComparedWithExistingValues(SCHWARZENEGGER));
  }

  @Test
  public void isLatestVersionWithTwoRootElementsAndOneChild() {
    // Given two versions in root and one in the child
    root.add(REAGAN_2);
    root.add(REAGAN_3);
    child.add(REAGAN_2);

    // At root then expect version 3, but not version 2 to return true
    assertTrue(root.isNewerOrSameVersionComparedWithExistingValues(REAGAN_3));
    assertFalse(root.isNewerOrSameVersionComparedWithExistingValues(REAGAN_2));

    // At the child on the other hand only version 2 exist, so we
    // expect version 2, but not version 1 to return true
    assertTrue(child.isNewerOrSameVersionComparedWithExistingValues(REAGAN_2));
    assertFalse(child.isNewerOrSameVersionComparedWithExistingValues(REAGAN));
  }
}
