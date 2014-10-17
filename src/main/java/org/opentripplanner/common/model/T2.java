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

import java.io.Serializable;

/**
 * An ordered pair of objects of potentially different types
 */
public class T2<E1, E2> implements Serializable {
    private static final long serialVersionUID = 1L;

    public final E1 first;

    public final E2 second;

    public T2(E1 first, E2 second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int hashCode() {
        return (first != null ? first.hashCode() : 0) + (second != null ? second.hashCode() : 0);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof T2)) return false;

        T2 other = (T2) object;

        if (first == null) {
            if (other.first != null) return false;
        } else {
            if (!first.equals(other.first)) return false;
        }

        if (second == null) {
            if (other.second != null) return false;
        } else {
            if (!second.equals(other.second)) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "T2(" + first + ", " + second + ")";
    }
}
