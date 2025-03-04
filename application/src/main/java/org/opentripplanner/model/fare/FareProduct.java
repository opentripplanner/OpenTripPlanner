package org.opentripplanner.model.fare;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.Sandbox;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A ticket that a user can purchase to travel.
 * <p>
 * It may be valid for the entirety of an itinerary or just for some of its legs.
 *
 * @param id       Identity of the product
 * @param name     Human-readable name of the product
 * @param price    The price of the fare product
 * @param validity Maximum duration that the product is valid for, if null then unlimited duration.
 * @param category Rider category, for example seniors or students
 * @param medium   Medium to "hold" the fare, like "cash", "HSL app" or
 */
@Sandbox
public record FareProduct(
  FeedScopedId id,
  String name,
  Money price,
  @Nullable Duration validity,
  @Nullable RiderCategory category,
  @Nullable FareMedium medium
) {
  public FareProduct {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
    Objects.requireNonNull(price);
  }

  public static FareProductBuilder of(FeedScopedId id, String name, Money price) {
    return new FareProductBuilder(id, name, price);
  }

  public boolean coversDuration(Duration journeyDuration) {
    return (Objects.nonNull(validity) && validity.toSeconds() > journeyDuration.toSeconds());
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(FareProduct.class)
      .addStr("id", id.toString())
      .addStr("name", name)
      .addObj("amount", price)
      .addDuration("duration", validity)
      .addObj("category", category)
      .addObj("medium", medium)
      .toString();
  }

  /**
   * Computes a unique ID for this product based on its id and properties.
   * <p>
   * This ID can then be used as a deduplication id to identify the fare product across legs.
   * <p>
   * For example, there can be two legs which have the fare products day pass and single ticket
   * each. However, the day passes have the same instance id meaning it's a single fare product for
   * both legs. The two single tickets have different instance ids and which means that the
   * passenger has to buy two of them.
   */
  public String uniqueInstanceId(ZonedDateTime startTime) {
    var buf = new StringBuilder();
    buf
      .append(startTime.toEpochSecond())
      .append(id)
      .append(price.currency().getCurrencyCode())
      .append(price.minorUnitAmount());

    if (validity != null) {
      buf.append(validity.toSeconds());
    }
    if (medium != null) {
      buf.append(medium.id()).append(medium.name());
    }
    if (category != null) {
      buf.append(category.id()).append(category.name());
    }
    return UUID.nameUUIDFromBytes(buf.toString().getBytes(StandardCharsets.UTF_8)).toString();
  }
}
