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

public class RideEstimateRequest {

    public Position startPosition;
    public Position endPosition;

    public RideEstimateRequest(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        this.startPosition = new Position(startLatitude, startLongitude);
        this.endPosition = new Position(endLatitude, endLongitude);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RideEstimateRequest request = (RideEstimateRequest) o;
        return startPosition.equals(request.startPosition) &&
            endPosition.equals(request.endPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            startPosition.getIntLat(),
            startPosition.getIntLon(),
            endPosition.getIntLat(),
            endPosition.getIntLon()
        );
    }
}
