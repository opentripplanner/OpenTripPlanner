package org.opentripplanner.api.resource;

import org.locationtech.jts.geom.Envelope;

/**
 * This class contains helpers for transforming between web mercator tiles and geometric coordinates
 * See <a href="http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames">OSM wiki</a> for details
 */
public class WebMercatorTile {

  /**
   * Implements https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Tile_numbers_to_lon./lat.
   */
  public static Envelope tile2Envelope(int x, int y, int zoom) {
    double maxLat = tile2lat(y, zoom);
    double minLat = tile2lat(y + 1, zoom);
    double minLon = tile2lon(x, zoom);
    double maxLon = tile2lon(x + 1, zoom);
    return new Envelope(maxLon, minLon, maxLat, minLat);
  }

  private static double tile2lon(int x, int z) {
    return (x / Math.pow(2.0, z)) * 360.0 - 180;
  }

  private static double tile2lat(int y, int z) {
    double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
    return Math.toDegrees(Math.atan(Math.sinh(n)));
  }
}
