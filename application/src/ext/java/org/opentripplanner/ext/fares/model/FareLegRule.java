package org.opentripplanner.ext.fares.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A rule set for matching a leg for fare calculation purposes.
 */
public final class FareLegRule implements Serializable {

  private final FeedScopedId id;
  private final Collection<FareProduct> fareProducts;

  @Nullable
  private final FareDistance fareDistance;

  @Nullable
  private final FeedScopedId legGroupId;

  @Nullable
  private final FeedScopedId networkId;

  @Nullable
  private final FeedScopedId fromAreaId;

  @Nullable
  private final FeedScopedId toAreaId;

  @Nullable
  private final Integer priority;

  public FareLegRule(FareLegRuleBuilder builder) {
    if (builder.fareProducts().isEmpty()) {
      throw new IllegalArgumentException("fareProducts must contain at least one value");
    }
    builder.fareProducts().forEach(Objects::requireNonNull);
    this.id = Objects.requireNonNull(builder.id());
    this.fareProducts = builder.fareProducts();
    this.legGroupId = builder.legGroupId();
    this.networkId = builder.networkId();
    this.fromAreaId = builder.fromAreaId();
    this.toAreaId = builder.toAreaId();
    this.fareDistance = builder.fareDistance();
    this.priority = builder.priority();
  }

  public String feedId() {
    return id.getFeedId();
  }

  public static FareLegRuleBuilder of(FeedScopedId id, FareProduct fp) {
    return new FareLegRuleBuilder(id, List.of(fp));
  }

  public static FareLegRuleBuilder of(FeedScopedId id, Collection<FareProduct> fp) {
    return new FareLegRuleBuilder(id, fp);
  }

  public FeedScopedId id() {
    return id;
  }

  public FeedScopedId legGroupId() {
    return legGroupId;
  }

  @Nullable
  public FeedScopedId networkId() {
    return networkId;
  }

  @Nullable
  public FeedScopedId fromAreaId() {
    return fromAreaId;
  }

  @Nullable
  public FeedScopedId toAreaId() {
    return toAreaId;
  }

  @Nullable
  public FareDistance fareDistance() {
    return fareDistance;
  }

  public OptionalInt priority() {
    if (priority == null) {
      return OptionalInt.empty();
    } else {
      return OptionalInt.of(priority);
    }
  }

  public Collection<FareProduct> fareProducts() {
    return fareProducts;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (FareLegRule) obj;
    return (
      Objects.equals(this.id, that.id) &&
      Objects.equals(this.legGroupId, that.legGroupId) &&
      Objects.equals(this.networkId, that.networkId) &&
      Objects.equals(this.fromAreaId, that.fromAreaId) &&
      Objects.equals(this.toAreaId, that.toAreaId) &&
      Objects.equals(this.fareDistance, that.fareDistance) &&
      Objects.equals(this.fareProducts, that.fareProducts)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      id,
      legGroupId,
      networkId,
      fromAreaId,
      toAreaId,
      fareDistance,
      fareProducts
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(FareLegRule.class).addStr("id", this.id.toString()).toString();
  }
}
