package org.opentripplanner.ext.fares.model;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.fare.FareProduct;

public class FareTransferRuleBuilder {

  private FeedScopedId id;
  private FeedScopedId fromLegGroup;
  private FeedScopedId toLegGroup;
  private int transferCount = FareTransferRule.UNLIMITED_TRANSFERS;
  private TimeLimit timeLimit;
  private Collection<FareProduct> fareProducts = List.of();

  FareTransferRuleBuilder() {}

  public FareTransferRuleBuilder withId(FeedScopedId id) {
    this.id = id;
    return this;
  }

  public FareTransferRuleBuilder withFromLegGroup(FeedScopedId fromLegGroup) {
    this.fromLegGroup = fromLegGroup;
    return this;
  }

  public FareTransferRuleBuilder withToLegGroup(FeedScopedId toLegGroup) {
    this.toLegGroup = toLegGroup;
    return this;
  }

  public FareTransferRuleBuilder withTransferCount(int transferCount) {
    this.transferCount = transferCount;
    return this;
  }

  public FareTransferRuleBuilder withTimeLimit(TimeLimitType type, Duration timeLimit) {
    this.timeLimit = new TimeLimit(type, timeLimit);
    return this;
  }

  public FareTransferRuleBuilder withFareProducts(Collection<FareProduct> fareProducts) {
    this.fareProducts = fareProducts;
    return this;
  }

  public FeedScopedId id() {
    return id;
  }

  public FeedScopedId fromLegGroup() {
    return fromLegGroup;
  }

  public FeedScopedId toLegGroup() {
    return toLegGroup;
  }

  public int transferCount() {
    return transferCount;
  }

  @Nullable
  public TimeLimit timeLimit() {
    return timeLimit;
  }

  public Collection<FareProduct> fareProducts() {
    return fareProducts;
  }

  public FareTransferRule build() {
    return new FareTransferRule(this);
  }
}
