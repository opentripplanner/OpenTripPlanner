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

package org.opentripplanner.analyst.request;

import com.vividsolutions.jts.geom.Coordinate;

import lombok.Getter;
import lombok.Setter;

/**
 * A request for a sample grid (of a SPT).
 * 
 * @author laurent
 */
public class SampleGridRequest {

    @Getter
    @Setter
    private int precisionMeters = 200;

    @Getter
    @Setter
    private int maxTimeSec = 0;

    @Getter
    @Setter
    private Coordinate coordinateOrigin;

    public SampleGridRequest() {
    }

    public String toString() {
        return String.format("<timegrid request, coordBase=%s precision=%d meters>",
                coordinateOrigin, precisionMeters);
    }
}
