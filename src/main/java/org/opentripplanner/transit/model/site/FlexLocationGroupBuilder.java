package org.opentripplanner.transit.model.site;

import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FlexLocationGroupBuilder
  extends AbstractEntityBuilder<FlexLocationGroup, FlexLocationGroupBuilder> {

  private I18NString name;

  FlexLocationGroupBuilder(FeedScopedId id) {
    super(id);
  }

  FlexLocationGroupBuilder(@Nonnull FlexLocationGroup original) {
    super(original);
    // Optional fields
    this.name = original.getName();
  }

  @Override
  protected FlexLocationGroup buildFromValues() {
    return new FlexLocationGroup(this);
  }

  public FlexLocationGroupBuilder withName(I18NString name) {
    this.name = name;
    return this;
  }

  public I18NString name() {
    return name;
  }
}
