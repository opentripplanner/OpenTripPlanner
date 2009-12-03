/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

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
