package org.opentripplanner.transit.model.site;

import java.util.Objects;
import java.util.Optional;
import java.util.function.IntSupplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Location corresponding to a location where riders may request pickup or drop off, defined in the
 * GTFS bundle.
 */

public class AreaStop
  extends AbstractTransitEntity<AreaStop, AreaStopBuilder>
  implements StopLocation {

  private final int index;
  private final I18NString name;

  private final boolean hasFallbackName;

  private final I18NString description;

  private final Geometry geometry;

  private final String zoneId;

  private final I18NString url;

  private final WgsCoordinate centroid;

  AreaStop(AreaStopBuilder builder) {
    super(builder.getId());
    this.index = builder.createIndex();
    // according to the spec stop location names are optional for flex zones so, we set the id
    // as the bogus name. *shrug*
    if (builder.name() == null) {
      this.name = new NonLocalizedString(builder.getId().toString());
      hasFallbackName = true;
    } else {
      this.name = builder.name();
      hasFallbackName = builder.hasFallbackName();
    }
    this.description = builder.description();
    this.url = builder.url();
    this.zoneId = builder.zoneId();
    this.geometry = builder.geometry();
    this.centroid = Objects.requireNonNull(builder.centroid());
  }

  public static AreaStopBuilder of(FeedScopedId id, IntSupplier indexCounter) {
    return new AreaStopBuilder(id, indexCounter);
  }

  @Override
  public int getIndex() {
    return index;
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

  @Nonnull
  @Override
  public StopType getStopType() {
    return StopType.FLEXIBLE_AREA;
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

  /**
   * Returns the geometry of area that defines the stop, in this case the same as getGeometry.
   */
  @Override
  public Optional<Geometry> getEncompassingAreaGeometry() {
    return Optional.of(geometry);
  }

  @Override
  public boolean isPartOfStation() {
    return false;
  }

  @Override
  public boolean isPartOfSameStationAs(StopLocation alternativeStop) {
    return false;
  }

  /**
   * Names for GTFS flex locations are optional therefore we set the id as the name. When this is
   * the case then this method returns true.
   */
  public boolean hasFallbackName() {
    return hasFallbackName;
  }

  @Override
  public boolean sameAs(@Nonnull AreaStop other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(name, other.getName()) &&
      Objects.equals(description, other.getDescription()) &&
      Objects.equals(geometry, other.getGeometry()) &&
      Objects.equals(url, other.url) &&
      Objects.equals(zoneId, other.zoneId)
    );
  }

  @Override
  @Nonnull
  public AreaStopBuilder copy() {
    return new AreaStopBuilder(this);
  }
}
