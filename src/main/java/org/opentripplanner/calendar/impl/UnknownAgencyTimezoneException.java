/* This file is copied from OneBusAway project. */
package org.opentripplanner.calendar.impl;

public class UnknownAgencyTimezoneException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnknownAgencyTimezoneException(String agencyName, String timezone) {
        super("unknown timezone \"" + timezone + "\" for agency \"" + agencyName + "\"");
    }
}
