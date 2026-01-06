package org.opentripplanner.ext.fares.model;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.time.LocalTime;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

/**
 * A fare timeframe which can model at what times on which dates a fare rule applies.
 */
public class Timeframe implements Serializable {

  private final FeedScopedId serviceId;
  private final LocalTime start;
  private final LocalTime end;

  public Timeframe(TimeframeBuilder timeFrameBuilder) {
    this.serviceId = requireNonNull(timeFrameBuilder.serviceId);
    this.start = requireNonNull(timeFrameBuilder.start);
    this.end = requireNonNull(timeFrameBuilder.end);
  }

  public static TimeframeBuilder of() {
    return new TimeframeBuilder();
  }

  public FeedScopedId serviceId() {
    return serviceId;
  }

  public LocalTime startTime() {
    return start;
  }

  public LocalTime endTime() {
    return end;
  }

  @Override
  public String toString() {
    return ValueObjectToStringBuilder.of()
      .addText("[")
      .addObj(start)
      .addText("-")
      .addObj(end)
      .addText(",")
      .addObj(serviceId)
      .addText("]")
      .toString();
  }
}
