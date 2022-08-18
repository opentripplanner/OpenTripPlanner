package org.opentripplanner.routing.api.request.refactor.request;

import java.time.Duration;
import org.opentripplanner.model.GenericLocation;

public class ViaLocation {
  private GenericLocation point;
  private boolean passThroughPoint = false;
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
