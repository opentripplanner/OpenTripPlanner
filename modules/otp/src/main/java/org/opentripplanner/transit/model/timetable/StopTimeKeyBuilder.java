package org.opentripplanner.transit.model.timetable;

import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class StopTimeKeyBuilder extends AbstractEntityBuilder<StopTimeKey, StopTimeKeyBuilder> {

  StopTimeKeyBuilder(FeedScopedId id) {
    super(id);
  }

  StopTimeKeyBuilder(StopTimeKey original) {
    super(original);
  }

  @Override
  protected StopTimeKey buildFromValues() {
    return new StopTimeKey(this);
  }
}
