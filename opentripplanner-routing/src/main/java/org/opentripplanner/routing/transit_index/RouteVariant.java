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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.transit_index.adapters.AgencyAndIdAdapter;
import org.opentripplanner.routing.transit_index.adapters.LineStringAdapter;
import org.opentripplanner.routing.transit_index.adapters.StopAgencyAndIdAdapter;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

/**
 * This represents a particular stop pattern on a particular route. For example, the N train has at
 * least four different variants: express (over the Manhattan bridge), and local (via lower
 * Manhattan and the tunnel) x to Astoria and to Coney Island. During construction, it sometimes has
 * a fifth variant: along the D line to Coney Island after 59th St (or from Coney Island to 59th).
 * 
 * This is needed because route names are intended for customer information, but scheduling
 * personnel need to know about where a particular trip actually goes.
 * 
 * @author novalis
 * 
 */
public class RouteVariant implements Serializable {
    private static final Logger _log = LoggerFactory.getLogger(RouteVariant.class);

    private static final long serialVersionUID = -3110443015998033630L;

    /*
     * This indicates that trips with multipledirection_ids are part of this variant. It should
     * probably never be used, because generally trips making the same stops in the same order will
     * have the same direction
     */
    private static final String MULTIDIRECTION = "[multidirection]";

    private String name; // "N via Whitehall"

    private TraverseMode mode;
    
    // @XmlElementWrapper
    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    private ArrayList<AgencyAndId> trips;

    @XmlJavaTypeAdapter(StopAgencyAndIdAdapter.class)
    private ArrayList<Stop> stops;

    /** An unordered list of all segments for this route */
    private ArrayList<RouteSegment> segments;

    /**
     * An ordered list of segments that represents one characteristic trip (or trip pattern) on this
     * variant
     */
    private ArrayList<RouteSegment> exemplarSegments;

    private Route route;

    private String direction;

    private LineString geometry;


    public RouteVariant() {
        // needed for JAXB but unused
    }

    public RouteVariant(Route route, ArrayList<Stop> stops) {
        this.route = route;
        this.stops = stops;
        trips = new ArrayList<AgencyAndId>();
        segments = new ArrayList<RouteSegment>();
        exemplarSegments = new ArrayList<RouteSegment>();
        this.mode = GtfsLibrary.getTraverseMode(route);
    }

    public void addTrip(Trip trip) {
        if (!trips.contains(trip.getId())) {
            trips.add(trip.getId());
            if (direction == null) {
                direction = trip.getDirectionId();
            } else {
                if (!direction.equals(trip.getDirectionId())) {
                    direction = MULTIDIRECTION;
                }
            }            
        }
    }

    public void addExemplarSegment(RouteSegment segment) {
        exemplarSegments.add(segment);
    }

    public void addSegment(RouteSegment segment) {
        segments.add(segment);
    }

    public List<RouteSegment> getSegments() {
        return segments;
    }

    public boolean isExemplarSet() {
        return !exemplarSegments.isEmpty();
    }

    public void cleanup() {
        trips.trimToSize();
        stops.trimToSize();
        exemplarSegments.trimToSize();

        // topological sort on segments to make sure that they are in order

        // since segments only know about their next edges, we must build a mapping from prior-edge
        // to segment; while we're at it, we find the first segment.
        HashMap<Edge, RouteSegment> successors = new HashMap<Edge, RouteSegment>();
        RouteSegment segment = null;
        for (RouteSegment s : exemplarSegments) {
            if (s.hopIn == null) {
                segment = s;
            } else {
                successors.put(s.hopIn, s);
            }
        }

        int i = 0;
        while (segment != null) {
            exemplarSegments.set(i++, segment);
            segment = successors.get(segment.hopOut);
        }
        if (i != exemplarSegments.size()) {
            _log.error("Failed to organize hops in route variant " + name);
        }
    }

    public List<RouteSegment> segmentsAfter(RouteSegment segment) {
        HashMap<Edge, RouteSegment> successors = new HashMap<Edge, RouteSegment>();
        for (RouteSegment s : segments) {
            if (s.hopIn != null) {
                successors.put(s.hopIn, s);
            }
        }

        //skip this seg
        segment = successors.get(segment.hopOut);
        ArrayList<RouteSegment> out = new ArrayList<RouteSegment>();
        while (segment != null) {
            out.add(segment);
            segment = successors.get(segment.hopOut);
        }
        return out;
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

    @XmlElement
    public String getDirection() {
        return direction;
    }


    @XmlJavaTypeAdapter(LineStringAdapter.class)
    public LineString getGeometry() {
        if (geometry == null) {
            List<Coordinate> coords = new ArrayList<Coordinate>();
            for (RouteSegment segment : exemplarSegments) {
                if (segment.hopOut != null) {
                    Geometry segGeometry = segment.getGeometry();
                    coords.addAll(Arrays.asList(segGeometry.getCoordinates()));
                }
            }
            Coordinate[] coordArray = new Coordinate[coords.size()];
            geometry = GeometryUtils.getGeometryFactory().createLineString(coords.toArray(coordArray));

        }
        return geometry;
    }

    public TraverseMode getTraverseMode() {
        return mode;
    }

    public void setGeometry(LineString geometry) {
        this.geometry = geometry;
    }
}
