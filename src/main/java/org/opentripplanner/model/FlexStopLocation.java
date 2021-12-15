package org.opentripplanner.model;

import java.util.Optional;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

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

  private Point centroid;

  public FlexStopLocation(FeedScopedId id) {
    super(id);
  }

  /**
   * Defines the name of the location. The name should be the same, which is used in customer
   * communication, eg. the name of the village where the service stops.
   */
  @Override
  public String getName() {
    // according to the spec stop location names are optional for flex zones so, we return the id
    // when it's null. *shrug*
    return Optional.ofNullable(name).orElse(getId().toString());
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getUrl() {
    return url;
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
  @Override
  public Geometry getGeometry() {
    return geometry;
  }

  public void setGeometry(Geometry geometry) {
    this.geometry = geometry;
    this.centroid = geometry.getCentroid();
  }

  /**
   * Returns the centroid of this location.
   */
  @Override
  public WgsCoordinate getCoordinate() {
    return new WgsCoordinate(centroid.getY(), centroid.getX());
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
  public boolean isPartOfSameStationAs(StopLocation alternativeStop) {
    return false;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
