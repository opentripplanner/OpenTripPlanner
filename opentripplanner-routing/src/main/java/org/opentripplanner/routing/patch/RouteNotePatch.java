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

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteSegment;
import org.opentripplanner.routing.transit_index.RouteVariant;

/**
 * This adds a note to all boardings of a given route (optionally, in a given
 * direction)
 * 
 * @author novalis
 * 
 */
@XmlRootElement(name = "RouteNotePatch")
public class RouteNotePatch extends Patch {
	private static final long serialVersionUID = -7947169269916558755L;

	private AgencyAndId route;

	private String direction;

	public RouteNotePatch() {
	}

	@Override
	public void remove(Graph graph) {
		TransitIndexService index = graph.getService(TransitIndexService.class);
		List<RouteVariant> variants = index.getVariantsForRoute(route);
		for (RouteVariant variant : variants) {
			if (direction != null && !direction.equals(variant.getDirection())) {
				continue;
			}
			for (RouteSegment segment : variant.getSegments()) {
				if (segment.board != null) {
					segment.board.removePatch(this);
				}
			}
		}
	}

	@Override
	public void apply(Graph graph) {
		TransitIndexService index = graph.getService(TransitIndexService.class);
		List<RouteVariant> variants = index.getVariantsForRoute(route);
		for (RouteVariant variant : variants) {
			if (direction != null && !direction.equals(variant.getDirection())) {
				continue;
			}
			for (RouteSegment segment : variant.getSegments()) {
				if (segment.board != null) {
					segment.board.addPatch(this);
				}
			}
		}
	}

	@Override
	public void filterTraverseResult(StateEditor result) {
		result.addNote(notes);
	}

	@XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
	public AgencyAndId getRoute() {
		return route;
	}

	public void setRoute(AgencyAndId route) {
		this.route = route;
	}

	public void setDirection(String direction) {
		if (direction != null && direction.equals("")) {
			direction = null;
		}
		this.direction = direction;
	}

	@XmlElement
	public String getDirection() {
		return direction;
	}

}
