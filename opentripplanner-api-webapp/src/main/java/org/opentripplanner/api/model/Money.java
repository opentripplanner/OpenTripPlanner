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

/**
 * <strong>Fare support has not yet been implemented.</strong>
 *
 */
public class Money {

    /**
     * The currency of the money.
     */
    Currency currency;
    /**
     * The actual currency value in decimal fixed-point, with the default number of fraction digits
     * from currency after the decimal point.
     */
    int cents; 

    public Money() {}
    
    public Money(Currency currency, int cents) {
        this.currency = currency;
        this.cents = cents;
    }
    

}
