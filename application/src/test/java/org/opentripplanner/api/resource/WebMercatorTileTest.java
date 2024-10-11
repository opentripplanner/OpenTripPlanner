package org.opentripplanner.api.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WebMercatorTileTest {

  @Test
  void tile2Envelope() {
    var wholeWorld = WebMercatorTile.tile2Envelope(0, 0, 0);

    assertEquals(180, wholeWorld.getMaxX());
    assertEquals(-180, wholeWorld.getMinX());
    assertEquals(85.0511, wholeWorld.getMaxY(), 0.0001);
    assertEquals(-85.0511, wholeWorld.getMinY(), 0.0001);

    var northEastQuarter = WebMercatorTile.tile2Envelope(1, 0, 1);

    assertEquals(180, northEastQuarter.getMaxX());
    assertEquals(0, northEastQuarter.getMinX());
    assertEquals(85.0511, northEastQuarter.getMaxY(), 0.0001);
    assertEquals(0, northEastQuarter.getMinY());
  }
}
