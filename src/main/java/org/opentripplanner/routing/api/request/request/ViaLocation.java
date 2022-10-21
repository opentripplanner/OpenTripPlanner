package org.opentripplanner.routing.api.request.request;

import java.time.Duration;
import org.opentripplanner.model.GenericLocation;

public record ViaLocation(
  GenericLocation point,
  boolean passThrougPoint,
  Duration minSlack,
  Duration maxSlack
) {}
