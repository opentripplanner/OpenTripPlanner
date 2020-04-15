/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

/**
 * A place where a station connects to the street network. Equivalent to GTFS stop location .
 */
public final class Entrance extends StationElement {

  @Override
  public String toString() {
    return "<Entrance " + this.id + ">";
  }
}
