package org.opentripplanner.street.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;

public class StreetTraversalPermissionTest {

  @Test
  public void testGetCode() {
    StreetTraversalPermission perm1 = StreetTraversalPermission.BICYCLE_AND_CAR;
    StreetTraversalPermission perm2 = StreetTraversalPermission.get(perm1.code);
    assertEquals(perm1, perm2);

    StreetTraversalPermission perm3 = StreetTraversalPermission.BICYCLE;
    assertNotEquals(perm1, perm3);
  }

  @Test
  public void testRemove() {
    StreetTraversalPermission perm1 = StreetTraversalPermission.CAR;
    StreetTraversalPermission none = perm1.remove(StreetTraversalPermission.CAR);
    assertEquals(StreetTraversalPermission.NONE, none);
  }

  @Test
  public void testAllowsStreetTraversalPermission() {
    StreetTraversalPermission perm1 = StreetTraversalPermission.ALL;
    assertTrue(perm1.allows(StreetTraversalPermission.CAR));
    assertTrue(perm1.allows(StreetTraversalPermission.BICYCLE));
    assertTrue(perm1.allows(StreetTraversalPermission.PEDESTRIAN));
    assertTrue(perm1.allows(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE));
  }

  @Test
  public void testAllowsTraverseMode() {
    StreetTraversalPermission perm1 = StreetTraversalPermission.ALL;
    assertTrue(perm1.allows(TraverseMode.CAR));
    assertTrue(perm1.allows(TraverseMode.WALK));
  }

  @Test
  public void testAllowsTraverseModeSet() {
    StreetTraversalPermission perm = StreetTraversalPermission.BICYCLE_AND_CAR;
    assertTrue(perm.allows(TraverseModeSet.allModes()));
    assertTrue(perm.allows(new TraverseModeSet(TraverseMode.CAR, TraverseMode.BICYCLE)));
    assertTrue(perm.allows(new TraverseModeSet(TraverseMode.BICYCLE)));
    assertFalse(perm.allows(new TraverseModeSet(TraverseMode.WALK)));
  }

  @Test
  public void testAllowsAnythingNothing() {
    StreetTraversalPermission perm = StreetTraversalPermission.CAR;
    assertTrue(perm.allowsAnything());
    assertFalse(perm.allowsNothing());

    perm = StreetTraversalPermission.NONE;
    assertFalse(perm.allowsAnything());
    assertTrue(perm.allowsNothing());
  }

  @Test
  public void testIntersect() {
    StreetTraversalPermission perm = StreetTraversalPermission.ALL;
    StreetTraversalPermission bike_walk = StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

    StreetTraversalPermission combined = perm.intersection(bike_walk);

    assertTrue(perm.allows(StreetTraversalPermission.ALL));
    assertTrue(combined.allows(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE));
    assertFalse(combined.allows(StreetTraversalPermission.CAR));
    assertTrue(bike_walk.allows(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE));
  }
}
