package org.opentripplanner.transit.model.site;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitEntity2;

/**
 * Location corresponding to a location where riders may request pickup or drop off, defined in the
 * GTFS bundle.
 */

public class FlexStopLocation
  extends TransitEntity2<FlexStopLocation, FlexStopLocationBuilder>
  implements StopLocation {

  private final I18NString name;

  private final boolean hasFallbackName;

  private I18NString description;

  private Geometry geometry;

  private String zoneId;

  private I18NString url;

  private WgsCoordinate centroid;

  FlexStopLocation(FlexStopLocationBuilder builder) {
    super(builder.getId());
    // according to the spec stop location names are optional for flex zones so, we set the id
    // as the bogus name. *shrug*
    if (builder.name() == null) {
      this.name = new NonLocalizedString(builder.getId().toString());
      hasFallbackName = true;
    } else {
      this.name = builder.name();
      hasFallbackName = false;
    }
  }

  public static FlexStopLocationBuilder of(FeedScopedId id) {
    return new FlexStopLocationBuilder(id);
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

  public void setZoneId(String zoneId) {
    this.zoneId = zoneId;
  }

  /**
   * Names for GTFS flex locations are optional therefore we set the id as the name. When this is
   * the case then this method returns true.
   */
  public boolean hasFallbackName() {
    return hasFallbackName;
  }

  @Override
  public boolean sameAs(@Nonnull FlexStopLocation other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(name, other.getName()) &&
      Objects.equals(description, other.getDescription()) &&
      Objects.equals(geometry, other.getGeometry())
    );
  }

  @Override
  @Nonnull
  public FlexStopLocationBuilder copy() {
    return new FlexStopLocationBuilder(this);
  }
}
