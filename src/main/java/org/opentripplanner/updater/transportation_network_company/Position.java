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

package org.opentripplanner.updater.transportation_network_company;

import java.util.Objects;

/**
 * This class is used for approximating a position.
 * It is used so that numerous TNC requests with very similar coordinates can be assumed to be the same.
 */
public class Position {
    public double latitude;
    public double longitude;

    private int intLat;
    private int intLon;

    public Position(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.intLat = intVal(latitude);
        this.intLon = intVal(longitude);
    }

    public int getIntLat() {
        return intLat;
    }

    public int getIntLon() {
        return intLon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return intLat == position.getIntLat() &&
            intLon == position.getIntLon();
    }

    @Override
    public int hashCode() {
        return Objects.hash(intLat, intLon);
    }

    public int intVal (double d) {
        return (int) (d * 10000);
    }
}
