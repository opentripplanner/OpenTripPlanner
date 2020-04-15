/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

/**
 * A place along a platform, where the vehicle van be boarded. Equivalent to GTFS stop location.
 */
public final class BoardingArea extends StationElement {

  private static final long serialVersionUID = 2L;

  private Stop parentStop;

  /**
   * Center point/location for the boarding area. Returns the coordinate of the parent stop,
   * if the coordinate is not defined for this boarding area.
   */
  public WgsCoordinate getCoordinate() {
    return coordinate != null ? coordinate : parentStop.getCoordinate();
  }

  /**
   * Returns the parent stop whis boarding area belongs to.
   */
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
