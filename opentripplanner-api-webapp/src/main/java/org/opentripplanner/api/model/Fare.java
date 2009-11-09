package org.opentripplanner.api.model;

import java.util.Hashtable;
import java.util.logging.Logger;

/**
 *
 */
public class Fare {
    protected static final Logger LOGGER = Logger.getLogger(Fare.class.getCanonicalName());

    protected static enum FareType {
        regular, student, senior, tram, special
    }

    public Hashtable<FareType, String> fare;

    public Fare() {
        fare = new Hashtable<FareType, String>();
        fare.put(FareType.regular, "$2.22");
        fare.put(FareType.senior, "$3.33");
        fare.put(FareType.student, "$4.44");
    }
}
