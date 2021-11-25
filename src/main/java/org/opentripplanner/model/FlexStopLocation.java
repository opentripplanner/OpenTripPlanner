package org.opentripplanner.model;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

/**
 * Location corresponding to a location where riders may request pickup or drop off, defined in the
 * GTFS bundle.
 */

public class FlexStopLocation extends TransitEntity implements StopLocation {
  private static final long serialVersionUID = 1L;

  private String name;

  private Geometry geometry;

  private String zoneId;

  public FlexStopLocation(FeedScopedId id) {
    super(id);
  }

  /**
   * Defines the name of the location. The name should be the same, which is used in customer
   * communication, eg. the name of the village where the service stops.
   */
  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the geometry of this location, can be any type of geometry.
   */
  public Geometry getGeometry() {
    return geometry;
  }

  public void setGeometry(Geometry geometry) {
    this.geometry = geometry;
  }

  /**
   * Returns the centroid of this location.
   */
  @Override
  public WgsCoordinate getCoordinate() {
    Point centroid = geometry.getCentroid();
    return new WgsCoordinate(centroid.getY(), centroid.getX());
  }

  @Override
  public String getFirstZoneAsString() {
    return zoneId;
  }

  public void setZoneId(String zoneId) {
    this.zoneId = zoneId;
  }
}
