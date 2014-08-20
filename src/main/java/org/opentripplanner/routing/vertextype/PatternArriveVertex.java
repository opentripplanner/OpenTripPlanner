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

package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;

public class PatternArriveVertex extends PatternStopVertex {

    private static final long serialVersionUID = 20140101;

    /** constructor for table trip patterns */
    public PatternArriveVertex(Graph g, TripPattern pattern, int stopIndex) {
        super(g, makeLabel(pattern, stopIndex), pattern, pattern.stopPattern.stops[stopIndex]);
    }

    // constructor for frequency patterns is now missing
    // it is possible to have both a freq and non-freq pattern with the same stop pattern

    private static String makeLabel(TripPattern pattern, int stop) {
        return String.format("%s_%02d_A", pattern.code, stop);
    }


}
