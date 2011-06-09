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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;

/**
 * This represents a particular stop pattern on a particular route. For example,
 * the N train has at least four different variants: express (over the Manhattan
 * bridge), and local (via lower Manhattan and the tunnel) x to Astoria and to
 * Coney Island. During construction, it sometimes has a fifth variant: along
 * the D line to Coney Island after 59th St (or from Coney Island to 59th).
 * 
 * This is needed because route names are intended for customer information, but
 * scheduling personnel need to know about where a particular trip actually
 * goes.
 * 
 * @author novalis
 * 
 */
public class RouteVariant implements Serializable {
	private static final long serialVersionUID = -3110443015998033630L;

	/*
	 * This indicates that trips with multipledirection_ids are part of this
	 * variant. It should probably never be used, because generally trips making
	 * the same stops in the same order will have the same direction
	 */
	private static final String MULTIDIRECTION = "[multidirection]";

	private String name; // "N via Whitehall"
	private ArrayList<AgencyAndId> trips;
	private ArrayList<Stop> stops;

	private ArrayList<RouteSegment> segments;
	private Route route;

	private String direction;

	public RouteVariant(Route route, ArrayList<Stop> stops) {
		this.route = route;
		this.stops = stops;
		trips = new ArrayList<AgencyAndId>();
		segments = new ArrayList<RouteSegment>();
	}

	public void addTrip(Trip trip) {
		trips.add(trip.getId());
		if (direction == null) {
			direction = trip.getDirectionId();
		} else {
			if (!direction.equals(trip.getDirectionId())) {
				direction = MULTIDIRECTION;
			}
		}
	}

	public void addSegment(RouteSegment segment) {
		segments.add(segment);
	}

	public List<RouteSegment> getSegments() {
		return segments;
	}

	public void cleanup() {
		trips.trimToSize();
		stops.trimToSize();
		segments.trimToSize();
	}

	public ArrayList<Stop> getStops() {
		return stops;
	}

	public Route getRoute() {
		return route;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<AgencyAndId> getTrips() {
		return trips;
	}

	public String toString() {
		return "RouteVariant(" + name + ")";
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public String getDirection() {
		return direction;
	}
}
