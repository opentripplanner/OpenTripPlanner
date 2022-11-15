package org.opentripplanner.ext.fares.model;

import java.time.Duration;
import java.util.Objects;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record FareProduct(
  FeedScopedId id,
  String name,
  Money amount,
  Duration duration,
  RiderCategory category,
  FareContainer container
) {
  public boolean coversDuration(Duration journeyDuration) {
    return Objects.nonNull(duration) && duration.toSeconds() > journeyDuration.toSeconds();
  }
}
