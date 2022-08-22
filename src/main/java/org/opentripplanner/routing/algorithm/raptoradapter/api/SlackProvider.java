package org.opentripplanner.routing.algorithm.raptoradapter.api;

import org.opentripplanner.transit.model.basic.TransitMode;

public interface SlackProvider {
  static int slackIndex(final TransitMode mode) {
    return org.opentripplanner.routing.algorithm.raptoradapter.transit.SlackProvider.slackIndex(
      mode
    );
  }
}
