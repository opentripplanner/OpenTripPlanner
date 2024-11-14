package org.opentripplanner.apis.gtfs.model;

import org.opentripplanner.transit.model.site.Entrance;

/**
 * A generic wrapper class for features in Walk steps.
 * At the moment only subway station entrances.
 **/
public class StepFeature {

  private final Object feature;

  public StepFeature(Entrance entrance) {
    this.feature = entrance;
  }

  public Object getFeature() {
    return feature;
  }
}
