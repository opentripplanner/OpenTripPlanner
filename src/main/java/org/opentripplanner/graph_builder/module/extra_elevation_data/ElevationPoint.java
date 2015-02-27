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

package org.opentripplanner.graph_builder.module.extra_elevation_data;

public class ElevationPoint implements Comparable<ElevationPoint> {
    public double distanceAlongShape, ele;

    public ElevationPoint(double distance, double ele) {
        this.distanceAlongShape = distance;
        this.ele = ele;
    }

    public ElevationPoint fromBack(double length) {
        return new ElevationPoint(length - distanceAlongShape, ele);
    }

    @Override
    public int compareTo(ElevationPoint arg0) {
        return (int) Math.signum(distanceAlongShape - arg0.distanceAlongShape);
    }
    
    public String toString() {
        return "ElevationPoint(" + distanceAlongShape + ", " + ele + ")";
    }

}
