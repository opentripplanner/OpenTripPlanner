package org.opentripplanner.ext.fares.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.utils.lang.IntUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public final class FareTransferRule implements Serializable {

  public static final int UNLIMITED_TRANSFERS = -1;
  private final FeedScopedId id;

  @Nullable
  private final FeedScopedId fromLegGroup;

  @Nullable
  private final FeedScopedId toLegGroup;

  private final int transferCount;

  private final Collection<FareProduct> fareProducts;

  @Nullable
  private final TimeLimit timeLimit;

  FareTransferRule(FareTransferRuleBuilder b) {
    this.id = Objects.requireNonNull(b.id());
    this.fareProducts = List.copyOf(b.fareProducts());
    this.fromLegGroup = b.fromLegGroup();
    this.toLegGroup = b.toLegGroup();
    this.transferCount = IntUtils.requireInRange(
      b.transferCount(),
      UNLIMITED_TRANSFERS,
      Integer.MAX_VALUE
    );
    this.timeLimit = b.timeLimit();
  }

  /**
   * Returns true if this rule contains a free transfer product or an empty list of fare products.
   */
  public boolean isFree() {
    return fareProducts.isEmpty() || fareProducts.stream().allMatch(p -> p.price().isZero());
  }

  public boolean containsWildCard() {
    return fromLegGroup == null || toLegGroup == null;
  }

  /**
   * Returns true if there is no limit on the number of transfers or if limit unknown.
   */
  public boolean unlimitedTransfers() {
    return transferCount == UNLIMITED_TRANSFERS;
  }

  /**
   * Does it limit the number of transfers?
   */
  public boolean limitedTransfers() {
    return !unlimitedTransfers();
  }

  /**
   * Does the rule allow a given number of transfers?
   */
  public boolean allowsNumberOfTransfers(int transferCount) {
    if (unlimitedTransfers()) {
      return true;
    }
    return this.transferCount >= transferCount;
  }

  public FeedScopedId id() {
    return id;
  }

  @Nullable
  public FeedScopedId fromLegGroup() {
    return fromLegGroup;
  }

  @Nullable
  public FeedScopedId toLegGroup() {
    return toLegGroup;
  }

  public Collection<FareProduct> fareProducts() {
    return fareProducts;
  }

  public Optional<TimeLimit> timeLimit() {
    return Optional.ofNullable(timeLimit);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (FareTransferRule) obj;
    return (
      Objects.equals(this.id, that.id) &&
      Objects.equals(this.fromLegGroup, that.fromLegGroup) &&
      Objects.equals(this.toLegGroup, that.toLegGroup) &&
      this.transferCount == that.transferCount &&
      Objects.equals(this.timeLimit, that.timeLimit) &&
      Objects.equals(this.fareProducts, that.fareProducts)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, fromLegGroup, toLegGroup, transferCount, timeLimit, fareProducts);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(FareTransferRule.class)
      .addObj("id", id)
      .addObj("fromLegGroup", fromLegGroup)
      .addObj("toLegGroup", toLegGroup)
      .addNum("transferCount", transferCount)
      .addObj("timeLimit", timeLimit)
      .addCol("fareProducts", fareProducts)
      .toString();
  }

  public static FareTransferRuleBuilder of() {
    return new FareTransferRuleBuilder();
  }
}
