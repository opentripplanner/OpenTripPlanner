/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.HashMap;
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

    public Fare() {
        fare = new HashMap<FareType, Money>();
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

    public Money getFare(FareType type) {
        return fare.get(type);
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
