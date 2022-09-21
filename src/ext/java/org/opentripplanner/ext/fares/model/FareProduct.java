package org.opentripplanner.ext.fares.model;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.Money;
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
