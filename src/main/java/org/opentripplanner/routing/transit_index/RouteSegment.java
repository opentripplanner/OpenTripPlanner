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

package org.opentripplanner.routing.transit_index;

import com.conveyal.geojson.GeometrySerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Geometry;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.graph.Edge;

import java.io.Serializable;

/**
 * RouteSegment holds the edges around one stop on particular trip or pattern.
 * We can use this if we patch the graph to remove the stop from the
 * trip/pattern.
 */
public class RouteSegment implements Serializable  {
	private static final long serialVersionUID = -3486047425509893460L;
	public Edge hopIn;
	public Edge hopOut;
	public Edge board;
	public Edge alight;
	public Edge dwell;
	public AgencyAndId stop;
	public RouteSegment(AgencyAndId stop) {
		this.stop = stop;
	}

	@JsonSerialize(using= GeometrySerializer.class)
	public Geometry getGeometry() {
	    return hopOut.getGeometry();
	}
}
