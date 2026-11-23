package org.opentripplanner.transit.model.site;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;

/**
 * A GTFS area ({@code areas.txt}): a group of stops, mainly used by GTFS Fares v2. Stops and
 * stations are assigned to areas through {@code stop_areas.txt}.
 * <p>
 * Not to be confused with {@link FareZone}, the GTFS {@code zone_id} used by Fares v1.
 */
public final class FareArea extends AbstractTransitEntity<FareArea, FareAreaBuilder> {

  @Nullable
  private final String name;

  FareArea(FareAreaBuilder builder) {
    super(builder.getId());
    this.name = builder.name();
  }

  public static FareAreaBuilder of(FeedScopedId id) {
    return new FareAreaBuilder(id);
  }

  /** The name of the area as displayed to the rider, GTFS {@code area_name}. */
  @Nullable
  public String getName() {
    return name;
  }

  @Override
  public FareAreaBuilder copy() {
    return new FareAreaBuilder(this);
  }

  @Override
  public boolean sameAs(FareArea other) {
    return getId().equals(other.getId()) && Objects.equals(name, other.name);
  }
}
