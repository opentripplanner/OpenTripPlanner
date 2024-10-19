package org.opentripplanner.transit.model.site;

import java.util.Objects;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A place where a station connects to the street network. Equivalent to GTFS stop location .
 */
public final class Entrance extends StationElement<Entrance, EntranceBuilder> {

  Entrance(EntranceBuilder builder) {
    super(builder);
    // Verify coordinate is not null
    Objects.requireNonNull(getCoordinate());
  }

  public static EntranceBuilder of(FeedScopedId id) {
    return new EntranceBuilder(id);
  }

  @Override
  public EntranceBuilder copy() {
    return new EntranceBuilder(this);
  }

  @Override
  public boolean sameAs(Entrance other) {
    return super.sameAs(other);
  }
}
