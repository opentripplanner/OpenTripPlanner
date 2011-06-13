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

package org.opentripplanner.routing.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteSegment;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatchServiceImpl implements PatchService {

	private GraphService graphService;
	private HashSet<String> patches = new HashSet<String>();

    @Autowired
    public void setGraphService(GraphService graphService) {
    	this.graphService = graphService;
    }

	@Override
	public Collection<Patch> getStopPatches(AgencyAndId stop) {
		Graph graph = graphService.getGraph();
		TransitIndexService index = graph.getService(TransitIndexService.class);
		
		HashSet<Patch> patches = new HashSet<Patch>();
		
		Edge edge = index.getPrealightEdge(stop);
		addAllPatchesFromEdge(patches, edge);
		
		edge = index.getPreboardEdge(stop);
		addAllPatchesFromEdge(patches, edge);

		return patches;
	}

	private void addAllPatchesFromEdge(HashSet<Patch> patches, Edge edge) {
		if (edge == null) {
			return;
		}
		List<Patch> edgePatches = edge.getPatches();
		if (edgePatches != null) {
			patches.addAll(edgePatches);
		}
	}

	@Override
	public synchronized void apply(Patch patch) {
		Graph graph = graphService.getGraph();
		if (!patches.contains(patch.getId())) {
			patch.apply(graph);
			patches.add(patch.getId());
		}
	}

	@Override
	public Collection<Patch> getRoutePatches(AgencyAndId route) {
		Graph graph = graphService.getGraph();
		TransitIndexService index = graph.getService(TransitIndexService.class);
		HashSet<Patch> patches = new HashSet<Patch>();
		
		for (RouteVariant variant : index.getVariantsForRoute(route)) {
			for (RouteSegment segment : variant.getSegments()) {
				addAllPatchesFrom(patches, segment.board);
				addAllPatchesFrom(patches, segment.alight);
				addAllPatchesFrom(patches, segment.hopIn);
				addAllPatchesFrom(patches, segment.dwell);
				addAllPatchesFrom(patches, segment.hopOut);
			}
		}
		
		return patches;
	}

	private void addAllPatchesFrom(HashSet<Patch> patches, Edge edge) {
		if (edge != null) {
			if (edge.getPatches() != null) {
				patches.addAll(edge.getPatches());
			}
		}
	}

}
