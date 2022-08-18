/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.site;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public final class Pathway extends AbstractTransitEntity<Pathway, PathwayBuilder> {

  private final PathwayMode pathwayMode;

  private final StationElement<?, ?> fromStop;

  private final StationElement<?, ?> toStop;

  private final String name;

  private final String reversedName;

  private final int traversalTime;

  private final double length;

  private final int stairCount;

  private final double slope;

  private final boolean isBidirectional;

  Pathway(PathwayBuilder builder) {
    super(builder.getId());
    // Required fields
    this.pathwayMode = Objects.requireNonNull(builder.pathwayMode());
    this.fromStop = Objects.requireNonNull(builder.fromStop());
    this.toStop = Objects.requireNonNull(builder.toStop());

    // Optional fields
    this.name = builder.name();
    this.reversedName = builder.reversedName();
    this.traversalTime = builder.traversalTime();
    this.length = builder.length();
    this.stairCount = builder.stairCount();
    this.slope = builder.slope();
    this.isBidirectional = builder.isBidirectional();
  }

  public static PathwayBuilder of(FeedScopedId id) {
    return new PathwayBuilder(id);
  }

  public PathwayMode getPathwayMode() {
    return pathwayMode;
  }

  public StationElement<?, ?> getFromStop() {
    return fromStop;
  }

  public StationElement<?, ?> getToStop() {
    return toStop;
  }

  public String getName() {
    return name;
  }

  public String getReversedName() {
    return reversedName;
  }

  public int getTraversalTime() {
    return traversalTime;
  }

  public double getLength() {
    return length;
  }

  public boolean isBidirectional() {
    return isBidirectional;
  }

  public int getStairCount() {
    return stairCount;
  }

  public double getSlope() {
    return slope;
  }

  public boolean isPathwayModeWheelchairAccessible() {
    return getPathwayMode() != PathwayMode.STAIRS && getPathwayMode() != PathwayMode.ESCALATOR;
  }

  @Override
  public boolean sameAs(@Nonnull Pathway other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(pathwayMode, other.getPathwayMode()) &&
      Objects.equals(fromStop, other.getFromStop()) &&
      Objects.equals(toStop, other.getToStop()) &&
      Objects.equals(name, other.getName()) &&
      Objects.equals(reversedName, other.getReversedName()) &&
      Objects.equals(traversalTime, other.getTraversalTime()) &&
      Objects.equals(length, other.getLength()) &&
      Objects.equals(stairCount, other.getStairCount()) &&
      Objects.equals(slope, other.getSlope()) &&
      Objects.equals(isBidirectional, other.isBidirectional())
    );
  }

  @Nonnull
  @Override
  public PathwayBuilder copy() {
    return new PathwayBuilder(this);
  }
}
