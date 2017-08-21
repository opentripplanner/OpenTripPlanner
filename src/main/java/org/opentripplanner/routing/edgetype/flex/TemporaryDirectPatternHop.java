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

package org.opentripplanner.routing.edgetype.flex;

import com.vividsolutions.jts.geom.LineString;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.vertextype.PatternStopVertex;

/**
 * This is associated with a PatternHop for stop_time information, but its geometry bears no
 * relation to the route geometry. And its timing is approximate.
 */
public class TemporaryDirectPatternHop extends TemporaryPartialPatternHop implements TemporaryEdge {
    private static final long serialVersionUID = 1L;

    private int time;

    public TemporaryDirectPatternHop(PatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, LineString geometry, int time) {
        super(hop, from, to, fromStop, toStop);
        setGeometry(geometry);
        this.time = time;
    }

    @Override
    public State traverse(State s0) {
        return super.traverse(s0);
    }

    @Override
    public boolean isUnscheduled() {
        return true;
    }

    @Override
    public boolean isTrivial() {
        return false;
    }

    @Override
    public double timeLowerBound(RoutingRequest options) {
        return time;
    }

    @Override
    public int getRunningTime(State s0) {
        return time;
    }

    @Override
    public void dispose() {
        fromv.removeOutgoing(this);
        tov.removeIncoming(this);
    }
}
