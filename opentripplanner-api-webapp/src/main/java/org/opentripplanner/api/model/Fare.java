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

package org.opentripplanner.api.model;

import java.util.Currency;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * <p><strong>Fare support has not yet been implemented.</strong>
 * </p><p>
 * Fare is a set of fares for different classes of users.</p>
 */
public class Fare {
    protected static final Logger LOGGER = Logger.getLogger(Fare.class.getCanonicalName());

    public static enum FareType {
        regular, student, senior, tram, special
    }

    /**
     * A mapping from {@link FareType} to {@link Money}.
     */
    public HashMap<FareType, Money> fare;

    public Fare() {
        fare = new HashMap<FareType, Money>();
    }

    public void addFare(FareType fareType, Currency currency, int cents) {
        fare.put(fareType, new Money(currency, cents));
    }
}
