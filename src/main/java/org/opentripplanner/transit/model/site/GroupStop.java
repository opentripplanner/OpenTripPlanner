package org.opentripplanner.transit.model.site;

import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A group of stopLocations, which can share a common Stoptime
 */
public class GroupStop
  extends AbstractTransitEntity<GroupStop, GroupStopBuilder>
  implements StopLocation {

  private final int index;
  private final Set<StopLocation> stopLocations;
  private final I18NString name;
  private final GeometryCollection geometry;

  private final WgsCoordinate centroid;

  GroupStop(GroupStopBuilder builder) {
    super(builder.getId());
    this.index = INDEX_COUNTER.getAndIncrement();
    this.name = builder.name();
    this.geometry = builder.geometry();
    this.centroid = builder.centroid();
    this.stopLocations = builder.stopLocations();
  }

  public static GroupStopBuilder of(FeedScopedId id) {
    return new GroupStopBuilder(id);
  }

  @Override
  public int getIndex() {
    return index;
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
  public boolean sameAs(@Nonnull GroupStop other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(name, other.getName()) &&
      Objects.equals(stopLocations, other.getLocations())
    );
  }

  @Override
  @Nonnull
  public GroupStopBuilder copy() {
    return new GroupStopBuilder(this);
  }
}
