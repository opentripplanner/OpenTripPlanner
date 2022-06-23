package org.opentripplanner.transit.model.site;

import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FareZoneBuilder extends AbstractEntityBuilder<FareZone, FareZoneBuilder> {

  private String name;

  FareZoneBuilder(FeedScopedId id) {
    super(id);
  }

  FareZoneBuilder(@Nonnull FareZone original) {
    super(original);
    this.name = original.getName();
  }

  public String name() {
    return name;
  }

  public FareZoneBuilder withName(String name) {
    this.name = name;
    return this;
  }

  @Override
  protected FareZone buildFromValues() {
    return new FareZone(this);
  }
}
