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

package org.opentripplanner.analyst.core;

import lombok.Getter;
import lombok.Setter;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A conveyor for an isochrone.
 * 
 * @author laurent
 */
public class IsochroneData {

    @Getter
    private int cutoffSec;

    @Getter
    private Geometry geometry;

    @Getter
    @Setter
    private Geometry debugGeometry;

    public IsochroneData(int cutoffSec, Geometry geometry) {
        this.cutoffSec = cutoffSec;
        this.geometry = geometry;
    }

    public String toString() {
        return String.format("<isochrone %s sec>", cutoffSec);
    }
}
