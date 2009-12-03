package org.opentripplanner.api.model;

import java.util.Currency;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 *
 */
public class Fare {
    protected static final Logger LOGGER = Logger.getLogger(Fare.class.getCanonicalName());

    public static enum FareType {
        regular, student, senior, tram, special
    }

    public Hashtable<FareType, Money> fare;

    public Fare() {
        fare = new Hashtable<FareType, Money>();
    }

    public void addFare(FareType fareType, Currency currency, int cents) {
        fare.put(fareType, new Money(currency, cents));
    }
}
