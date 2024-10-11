/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.calendar.impl;

public class UnknownAgencyTimezoneException extends RuntimeException {

  public UnknownAgencyTimezoneException(String agencyName, String timezone) {
    super("unknown timezone \"" + timezone + "\" for agency \"" + agencyName + "\"");
  }
}
