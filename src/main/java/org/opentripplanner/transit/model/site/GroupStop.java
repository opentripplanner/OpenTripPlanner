package org.opentripplanner.transit.model.site;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntSupplier;
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
  private final List<StopLocation> stopLocations;
  private final I18NString name;
  private final GeometryCollection geometry;

  private final GeometryCollection encompassingAreaGeometry;

  private final WgsCoordinate centroid;

  GroupStop(GroupStopBuilder builder) {
    super(builder.getId());
    this.index = builder.createIndex();
    this.name = builder.name();
    this.geometry = builder.geometry();
    this.centroid = Objects.requireNonNull(builder.centroid());
    this.stopLocations = builder.stopLocations();
    this.encompassingAreaGeometry = builder.encompassingAreaGeometry();
  }

  public static GroupStopBuilder of(FeedScopedId id, IntSupplier indexCounter) {
    return new GroupStopBuilder(id, indexCounter);
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
  @Nonnull
  public StopType getStopType() {
    return StopType.FLEXIBLE_GROUP;
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

  /**
   * Returns the geometry of all stops and areas belonging to this location group.
   */
  @Override
  public Geometry getGeometry() {
    return geometry;
  }

  /**
   * Returns the geometry of the area that encompasses the bounds of this StopLocation group. If the
   * group is defined as all the stops within an area, then this will return the geometry of the
   * area. If the group is defined simply as a list of stops, this will return an empty optional.
   */
  @Override
  public Optional<? extends Geometry> getEncompassingAreaGeometry() {
    return Optional.ofNullable(encompassingAreaGeometry);
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
   * Returns all the locations belonging to this location group.
   */
  @Override
  @Nonnull
  public List<StopLocation> getChildLocations() {
    return stopLocations;
  }

  @Override
  public boolean sameAs(@Nonnull GroupStop other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(name, other.getName()) &&
      Objects.equals(stopLocations, other.getChildLocations())
    );
  }

  @Override
  @Nonnull
  public GroupStopBuilder copy() {
    return new GroupStopBuilder(this);
  }
}
