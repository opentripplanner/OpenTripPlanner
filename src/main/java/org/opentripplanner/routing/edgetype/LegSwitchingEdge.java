/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.LineString;
import java.util.Locale;

/* This edge, because it has no mode, initiates another leg.
 */
public class LegSwitchingEdge extends Edge {
	private static final long serialVersionUID = 1L;

	public LegSwitchingEdge(Vertex v1, Vertex v2) {
        super(new Vertex(null, null, 0.0, 0.0) {}, new Vertex(null, null, 0.0, 0.0) {});
        fromv = v1;
        tov = v2;
        // Why is this code so dirty? Because we don't want this edge to be added to the edge lists.
	}

	@Override
	public State traverse(State s0) {
		StateEditor editor = s0.edit(this);
		editor.setBackMode(TraverseMode.LEG_SWITCH);
		return editor.makeState();
	}

	@Override
	public double getDistance() {
		return 0;
	}

	@Override
	public LineString getGeometry() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

        @Override
        public String getName(Locale locale) {
            return this.getName();
        }

}
