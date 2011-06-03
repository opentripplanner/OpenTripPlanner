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

package org.opentripplanner.routing.patch;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.services.TransitIndexService;

public class StopNotePatch extends AbstractPatch {
	private static final long serialVersionUID = -7947169269916558755L;
	
	private AgencyAndId stop;

	public StopNotePatch(long startTime, long endTime, int startTimeOfDay,
			int endTimeOfDay, String notes, AgencyAndId stop) {
		super(startTime, endTime, startTimeOfDay, endTimeOfDay, notes);
		this.stop = stop;
	}
	
	@Override
	public void remove(Graph graph) {
		TransitIndexService index = graph.getService(TransitIndexService.class);
		Edge edge = index.getPreboardEdge(stop);
		edge.removePatch(this);
		edge = index.getPrealightEdge(stop);
		edge.removePatch(this);
	}

	@Override
	public void apply(Graph graph) {
		TransitIndexService index = graph.getService(TransitIndexService.class);
		Edge edge = index.getPreboardEdge(stop);
		edge.addPatch(this);
		edge = index.getPrealightEdge(stop);
		edge.addPatch(this);
	}

	@Override
	public TraverseResult addTraverseResult(Edge edge, State s0,
			TraverseOptions wo) {
		return null;
	}

	@Override
	public TraverseResult addTraverseResultBack(Edge edge, State s0,
			TraverseOptions wo) {
		return null;
	}

	@Override
	public TraverseResult filterTraverseResults(TraverseResult result) {
		result = AbstractPatch.filterTraverseResultChain(result, new TraverseResultFilter() {
			public TraverseResult filter(TraverseResult result) {
				return new TraverseResult(result.weight, result.state, 
					new NoteNarrative(result.getEdgeNarrative(), notes));
			}
		}); 
			
			
		return result;
	}
	
	
}
