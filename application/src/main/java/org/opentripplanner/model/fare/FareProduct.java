package org.opentripplanner.model.fare;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
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
 */
@Sandbox
public final class FareProduct implements Serializable {

  private final FeedScopedId id;
  private final String name;
  private final Money price;

  @Nullable
  private final RiderCategory category;

  @Nullable
  private final FareMedium medium;

  FareProduct(FareProductBuilder builder) {
    this.id = Objects.requireNonNull(builder.id());
    this.name = Objects.requireNonNull(builder.name());
    this.price = Objects.requireNonNull(builder.price());
    this.category = builder.category();
    this.medium = builder.medium();
  }

  public static FareProductBuilder of(FeedScopedId id, String name, Money price) {
    return new FareProductBuilder(id, name, price);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(FareProduct.class)
      .addStr("id", id.toString())
      .addStr("name", name)
      .addObj("amount", price)
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

    if (medium != null) {
      buf.append(medium.id()).append(medium.name());
    }
    if (category != null) {
      buf.append(category.id()).append(category.name());
    }
    return UUID.nameUUIDFromBytes(buf.toString().getBytes(StandardCharsets.UTF_8)).toString();
  }

  public FeedScopedId id() {
    return id;
  }

  public String name() {
    return name;
  }

  public Money price() {
    return price;
  }

  public boolean isFree() {
    return price.isZero();
  }

  @Nullable
  public RiderCategory category() {
    return category;
  }

  @Nullable
  public FareMedium medium() {
    return medium;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (FareProduct) obj;
    return (
      Objects.equals(this.id, that.id) &&
      Objects.equals(this.name, that.name) &&
      Objects.equals(this.price, that.price) &&
      Objects.equals(this.category, that.category) &&
      Objects.equals(this.medium, that.medium)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, price, category, medium);
  }
}
