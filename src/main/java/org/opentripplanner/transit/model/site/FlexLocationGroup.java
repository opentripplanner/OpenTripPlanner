package org.opentripplanner.transit.model.site;

import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitEntity;

/**
 * A group of stopLocations, which can share a common Stoptime
 */
public class FlexLocationGroup
  extends TransitEntity<FlexLocationGroup, FlexLocationGroupBuilder>
  implements StopLocation {

  private final Set<StopLocation> stopLocations;
  private final I18NString name;
  private final GeometryCollection geometry;

  private final WgsCoordinate centroid;

  FlexLocationGroup(FlexLocationGroupBuilder builder) {
    super(builder.getId());
    this.name = builder.name();
    this.geometry = builder.geometry();
    this.centroid = builder.centroid();
    this.stopLocations = builder.stopLocations();
  }

  public static FlexLocationGroupBuilder of(FeedScopedId id) {
    return new FlexLocationGroupBuilder(id);
  }

  @Override
  public I18NString getName() {
    return name;
  }

  @Override
  public I18NString getDescription() {
    return null;
  }

  @Override
  @Nullable
  public I18NString getUrl() {
    return null;
  }

  @Override
  public String getFirstZoneAsString() {
    return null;
  }

  /**
   * Returns the centroid of all stops and areas belonging to this location group.
   */
  @Override
  @Nonnull
  public WgsCoordinate getCoordinate() {
    return centroid;
  }

  @Override
  public Geometry getGeometry() {
    return geometry;
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
   * Adds a new location to the location group. This should ONLY be used during the graph build
   * process.
   */

  /**
   * Returns all the locations belonging to this location group.
   */
  public Set<StopLocation> getLocations() {
    return stopLocations;
  }

  @Override
  public boolean sameAs(@Nonnull FlexLocationGroup other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(name, other.getName()) &&
      Objects.equals(stopLocations, other.getLocations())
    );
  }

  @Override
  @Nonnull
  public FlexLocationGroupBuilder copy() {
    return new FlexLocationGroupBuilder(this);
  }
}
