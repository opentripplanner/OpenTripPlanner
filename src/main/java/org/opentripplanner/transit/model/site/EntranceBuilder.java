package org.opentripplanner.transit.model.site;

import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A place where a station connects to the street network. Equivalent to GTFS stop location .
 */
public final class EntranceBuilder extends StationElementBuilder<Entrance, EntranceBuilder> {

  EntranceBuilder(FeedScopedId id) {
    super(id);
  }

  EntranceBuilder(Entrance original) {
    super(original);
  }

  @Override
  EntranceBuilder instance() {
    return this;
  }

  @Override
  protected Entrance buildFromValues() {
    return new Entrance(this);
  }
}
