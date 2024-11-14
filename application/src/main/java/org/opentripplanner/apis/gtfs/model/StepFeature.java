package org.opentripplanner.apis.gtfs.model;

import org.opentripplanner.transit.model.site.Entrance;

/**
 * A generic wrapper class for features in Walk steps.
 * At the moment only subway station entrances.
 **/
public class StepFeature {

  private final Entrance entranceFeature;

  public StepFeature(Entrance entrance) {
    this.entranceFeature = entrance;
  }

  public Entrance getEntrance() {
    return entranceFeature;
  }
}
