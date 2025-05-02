package org.opentripplanner.model.fare;

import java.time.Duration;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Builder for {@link FareProduct}.
 */
public class FareProductBuilder {

  private final FeedScopedId id;
  private final String name;
  private final Money price;
  private Duration validity;
  private RiderCategory category;
  private FareMedium medium;

  public FareProductBuilder(FeedScopedId id, String name, Money price) {
    this.id = id;
    this.name = name;
    this.price = price;
  }

  public FeedScopedId id() {
    return id;
  }

  public FareProductBuilder withValidity(Duration validity) {
    this.validity = validity;
    return this;
  }

  public FareProductBuilder withCategory(RiderCategory category) {
    this.category = category;
    return this;
  }

  public FareProductBuilder withMedium(FareMedium medium) {
    this.medium = medium;
    return this;
  }

  public String name() {
    return name;
  }

  public Money price() {
    return price;
  }

  public Duration validity() {
    return validity;
  }

  public RiderCategory category() {
    return category;
  }

  public FareMedium medium() {
    return medium;
  }

  public FareProduct build() {
    return new FareProduct(this);
  }
}
