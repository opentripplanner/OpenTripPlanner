package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Fare is a set of fares for different classes of users.
 * </p>
 */
public class Fare {

    public static enum FareType implements Serializable {
        regular, student, senior, tram, special, youth
    }

    /**
     * A mapping from {@link FareType} to {@link Money}.
     */
    public HashMap<FareType, Money> fare;

    /**
     * A mapping from {@link FareType} to a list of {@link FareComponent}.
     * The FareComponents are stored in an array instead of a list because JAXB doesn't know how to deal with
     * interfaces when serializing a trip planning response, and List is an interface.
     * See https://stackoverflow.com/a/1119241/778449
     */
    public HashMap<FareType, FareComponent[]> details;

    public Fare() {
        fare = new HashMap<>();
        details = new HashMap<>();
    }

    public Fare(Fare aFare) {
        this();
        if (aFare != null) {
            for (Map.Entry<FareType, Money> kv : aFare.fare.entrySet()) {
                fare.put(kv.getKey(), new Money(kv.getValue().getCurrency(), kv.getValue()
                        .getCents()));
            }
        }
    }

    public void addFare(FareType fareType, WrappedCurrency currency, int cents) {
        fare.put(fareType, new Money(currency, cents));
    }

    public void addFare(FareType fareType, Money money) {
        fare.put(fareType, money);
    }

    public void addFareDetails(FareType fareType, List<FareComponent> newDetails) {
        details.put(fareType, newDetails.toArray(new FareComponent[newDetails.size()]));
    }

    public Money getFare(FareType type) {
        return fare.get(type);
    }

    public List<FareComponent> getDetails(FareType type) {
        return Arrays.asList(details.get(type));
    }

    public void addCost(int surcharge) {
        for (Money cost : fare.values()) {
            int cents = cost.getCents();
            cost.setCents(cents + surcharge);
        }
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer("Fare(");
        for (FareType type : fare.keySet()) {
            Money cost = fare.get(type);
            buffer.append("[");
            buffer.append(type.toString());
            buffer.append(":");
            buffer.append(cost.toString());
            buffer.append("], ");
        }
        buffer.append(")");
        return buffer.toString();
    }
}
