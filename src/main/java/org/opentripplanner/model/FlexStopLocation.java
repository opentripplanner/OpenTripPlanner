package org.opentripplanner.model;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;

/**
 * Location corresponding to a location where riders may request pickup or drop off, defined in the
 * GTFS bundle.
 */

public class FlexStopLocation extends TransitEntity implements StopLocation {
  private static final long serialVersionUID = 1L;

  private String name;

  private String description;

  private Geometry geometry;

  private String zoneId;

  private String url;

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

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public double getLat() {
    return geometry.getCentroid().getX();
  }

  @Override
  public double getLon() {
    return geometry.getCentroid().getY();
  }

  public void setUrl(String url) {
    this.url = url;
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
  public GeometryCollection getGeometries() {
    return GeometryUtils.makeCollection(geometry);
  }

  @Override
  public String getFirstZoneAsString() {
    return zoneId;
  }

  public void setZoneId(String zoneId) {
    this.zoneId = zoneId;
  }

  @Override
  public boolean isPartOfStation() {
    return false;
  }

  @Override
  public StopTransferPriority getPriority() {
    return null;
  }

  @Override
  public boolean isPartOfSameStationAs(StopLocation alternativeStop) {
    return false;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
