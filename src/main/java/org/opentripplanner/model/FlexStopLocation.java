package org.opentripplanner.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitEntity;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.util.I18NString;

/**
 * Location corresponding to a location where riders may request pickup or drop off, defined in the
 * GTFS bundle.
 */

public class FlexStopLocation extends TransitEntity implements StopLocation {

  private I18NString name;

  private I18NString description;

  private Geometry geometry;

  private String zoneId;

  private I18NString url;

  private WgsCoordinate centroid;

  public FlexStopLocation(FeedScopedId id) {
    super(id);
  }

  /**
   * Defines the name of the location. The name should be the same, which is used in customer
   * communication, eg. the name of the village where the service stops.
   */
  @Override
  @Nonnull
  public I18NString getName() {
    return name;
  }

  @Override
  public I18NString getDescription() {
    return description;
  }

  @Override
  @Nullable
  public I18NString getUrl() {
    return url;
  }

  public void setUrl(I18NString url) {
    this.url = url;
  }

  @Override
  public String getFirstZoneAsString() {
    return zoneId;
  }

  /**
   * Returns the centroid of this location.
   */
  @Override
  @Nonnull
  public WgsCoordinate getCoordinate() {
    return centroid;
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
    this.centroid = new WgsCoordinate(geometry.getCentroid().getY(), geometry.getCentroid().getX());
  }

  @Override
  public boolean isPartOfStation() {
    return false;
  }

  @Override
  public boolean isPartOfSameStationAs(StopLocation alternativeStop) {
    return false;
  }

  public void setDescription(I18NString description) {
    this.description = description;
  }

  public void setName(I18NString name) {
    this.name = name;
  }

  public void setZoneId(String zoneId) {
    this.zoneId = zoneId;
  }
}
