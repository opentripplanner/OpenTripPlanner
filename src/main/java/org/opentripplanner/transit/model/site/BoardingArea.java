/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.site;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A place along a platform, where the vehicle van be boarded. Equivalent to GTFS stop location.
 */
public final class BoardingArea extends StationElement<BoardingArea, BoardingAreaBuilder> {

  private final RegularStop parentStop;

  BoardingArea(BoardingAreaBuilder builder) {
    super(builder);
    this.parentStop = Objects.requireNonNull(builder.parentStop());

    // Verify coordinate is not null
    Objects.requireNonNull(getCoordinate());
  }

  public static BoardingAreaBuilder of(FeedScopedId id) {
    return new BoardingAreaBuilder(id);
  }

  /**
   * Center point/location for the boarding area. Returns the coordinate of the parent stop,
   * if the coordinate is not defined for this boarding area.
   */
  @Override
  @Nonnull
  public WgsCoordinate getCoordinate() {
    return isCoordinateSet() ? super.getCoordinate() : parentStop.getCoordinate();
  }

  /**
   * Returns the parent stop this boarding area belongs to.
   */
  @Nonnull
  public RegularStop getParentStop() {
    return parentStop;
  }

  @Override
  @Nonnull
  public BoardingAreaBuilder copy() {
    return new BoardingAreaBuilder(this);
  }

  @Override
  public boolean sameAs(@Nonnull BoardingArea other) {
    return super.sameAs(other) && Objects.equals(parentStop, other.parentStop);
  }
}
