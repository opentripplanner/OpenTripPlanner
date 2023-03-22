package org.opentripplanner.ext.fares.model;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A ticket that a user can purchase to travel.
 * <p>
 * It may be valid for the entirety of an itinerary or just for some of its legs.
 *
 * @param id       Identity for the
 * @param name     Human-readable name of the product
 * @param amount   Price
 * @param duration Maximum duration of the product, if null then unlimited duration
 * @param category Rider category, for example seniors or students
 * @param medium   Medium to "hold" the fare, like "cash", "HSL app" or
 */
public record FareProduct(
  FeedScopedId id,
  String name,
  Money amount,
  Duration duration,
  RiderCategory category,
  FareMedium medium
) {
  public FareProduct {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
    Objects.requireNonNull(amount);
  }

  public boolean coversDuration(Duration journeyDuration) {
    return Objects.nonNull(duration) && duration.toSeconds() > journeyDuration.toSeconds();
  }

  public String uniqueCompositeUUID(ZonedDateTime startTime) {
    var builder = new StringBuilder()
      .append(id)
      .append(amount)
      .append(duration)
      .append(category)
      .append(medium)
      .append(startTime.toEpochSecond());
    return UUID.nameUUIDFromBytes(builder.toString().getBytes(StandardCharsets.UTF_8)).toString();
  }
}
