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

package org.opentripplanner.common.model;

/**
 * An ordered pair of objects of the same type
 *
 * @param <E>
 */
public class P2<E> extends T2<E, E> {

    private static final long serialVersionUID = 1L;

    public static <E> P2<E> createPair(E first, E second) {
        return new P2<E>(first, second);
    }

    public P2(E first, E second) {
        super(first, second);
    }
    
    public P2(E[] entries) {
        super(entries[0], entries[1]);
        if (entries.length != 2) {
            throw new IllegalArgumentException("This only takes arrays of 2 arguments");
        }
    }

    public String toString() {
        return "P2(" + first + ", " + second + ")";
    }
}
