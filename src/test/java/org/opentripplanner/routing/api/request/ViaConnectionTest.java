package org.opentripplanner.routing.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class ViaConnectionTest {

  private static final FeedScopedId ID = FeedScopedId.ofNullable("F", "1");

  private final ViaConnection connectionWithLocationId = new ViaConnection(ID);
  private final ViaConnection connectionWithCoordinate = new ViaConnection(WgsCoordinate.GREENWICH);

  @Test
  void hasLocationId() {
    assertTrue(connectionWithLocationId.hasLocationId());
    assertFalse(connectionWithCoordinate.hasLocationId());
  }

  @Test
  void hasCoordinate() {
    assertFalse(connectionWithLocationId.hasCoordinate());
    assertTrue(connectionWithCoordinate.hasCoordinate());
  }

  @Test
  void locationId() {
    assertEquals(ID, connectionWithLocationId.locationId());
    assertNull(connectionWithCoordinate.locationId());
  }

  @Test
  void coordinate() {
    assertNull(connectionWithLocationId.coordinate());
    assertEquals(WgsCoordinate.GREENWICH, connectionWithCoordinate.coordinate());
  }
}
