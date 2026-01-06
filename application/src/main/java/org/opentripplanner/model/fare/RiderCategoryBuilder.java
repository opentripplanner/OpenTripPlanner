package org.opentripplanner.model.fare;

import org.opentripplanner.core.model.id.FeedScopedId;

public class RiderCategoryBuilder {

  final FeedScopedId id;
  String name;
  boolean isDefault = false;

  RiderCategoryBuilder(FeedScopedId id) {
    this.id = id;
  }

  public RiderCategoryBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public RiderCategoryBuilder withIsDefault(boolean isDefault) {
    this.isDefault = isDefault;
    return this;
  }

  public RiderCategory build() {
    return new RiderCategory(this);
  }
}
