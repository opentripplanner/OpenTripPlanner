/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

/**
 * A place along a platform, where the vehicle van be boarded. Equivalent to GTFS stop location.
 */
public final class BoardingArea extends StationElement {

  private static final long serialVersionUID = 2L;

  private Stop parentStop;

  public WgsCoordinate getCoordinate() {
    return coordinate != null ? coordinate : parentStop.getCoordinate();
  }

  public Stop getParentStop() {
    return parentStop;
  }

  public void setParentStop(Stop parentStop) {
    this.parentStop = parentStop;
  }

  @Override
  public String toString() {
    return "<Entrance " + this.id + ">";
  }
}
