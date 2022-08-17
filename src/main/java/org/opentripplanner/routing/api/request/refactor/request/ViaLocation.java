package org.opentripplanner.routing.api.request.refactor.request;

import java.time.Duration;
import org.opentripplanner.model.GenericLocation;

public class ViaLocation {
  GenericLocation point;
  boolean passThroughPoint = false;
  Duration slack = Duration.ofMinutes(60);
}
