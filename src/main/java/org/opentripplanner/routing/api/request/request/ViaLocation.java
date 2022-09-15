package org.opentripplanner.routing.api.request.request;

import java.time.Duration;
import org.opentripplanner.model.GenericLocation;

// TODO VIA: JavaDoc
public class ViaLocation {

  private GenericLocation point;
  private boolean passThroughPoint = false;

  // TODO VIA Part 2: Is this minimum, should it be named so?
  private Duration slack = Duration.ofMinutes(60);

  public GenericLocation point() {
    return point;
  }

  public boolean passThroughPoint() {
    return passThroughPoint;
  }

  public Duration slack() {
    return slack;
  }
}
