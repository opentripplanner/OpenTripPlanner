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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.api.adapters.LineStringAdapter;
import org.opentripplanner.api.adapters.StopAgencyAndIdAdapter;
import org.opentripplanner.api.adapters.TripsModelInfo;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.json_serialization.EncodedPolylineJSONSerializer;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.graph.Edge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

/**
 * 
 * THIS CLASS IS BEING MERGED INTO STOPPATTERN / TRIPPATTERN. It is being kept around for reference
 * and is expected to be removed eventually.
 * 
 * A "route variant" represents a particular stop pattern on a particular route. For example, the N
 * train has at least four different variants: express (over the Manhattan bridge), and local (via
 * lower Manhattan and the tunnel) x to Astoria and to Coney Island. During construction, it
 * sometimes has a fifth variant: along the D line to Coney Island after 59th St (or from Coney
 * Island to 59th).
 * 
 * Route names are intended for very general customer information, but sometimes there is a need to
 * know where a particular trip actually goes.
 * 
 * Route Variant names are guaranteed to be unique (among variants for a single route) but not stable
 * across graph builds especially based on different GTFS inputs. They are
 * machine-generated on a best-effort basis. For instance, if a variant is the
 * only variant of the N that ends at Coney Island, the name will be "N to Coney Island". But if
 * multiple variants end at Coney Island (but have different stops elsewhere), that name would not
 * be chosen. OTP also tries start and intermediate stations ("from Coney Island", or "via
 * Whitehall", or even combinations ("from Coney Island via Whitehall"). But if there is no way to
 * create a unique name from start/end/intermediate stops, then the best we can do is to create a
 * "like [trip id]" name, which at least tells you where in the GTFS you can find a related trip.
 * 
 * @author novalis
 * 
 */
@XmlRootElement(name = "RouteVariant")
public class RouteVariant implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(RouteVariant.class);

    private static final long serialVersionUID = -3110443015998033630L;

    /*
     * This indicates that trips with multipledirection_ids are part of this variant. It should probably never be used, because generally trips making
     * the same stops in the same order will have the same direction
     */
    private static final String MULTIDIRECTION = "[multidirection]";

    private String name; // "N via Whitehall"

    private TraverseMode mode;

    private ArrayList<TripsModelInfo> trips;

    private ArrayList<Stop> stops;

    /** An unordered list of all segments for this route */
    @JsonIgnore
    private ArrayList<RouteSegment> segments;

    /**
     * An ordered list of segments that represents one characteristic trip (or trip pattern) on this variant
     */
    @JsonIgnore
    private ArrayList<RouteSegment> exemplarSegments;

    @JsonIgnore
    private ArrayList<PatternInterlineDwell> interlines;

    private Route route;

    private String direction;

    private LineString geometry;

    public RouteVariant() {
        // needed for JAXB but unused
    }

    public RouteVariant(Route route, ArrayList<Stop> stops) {
        this.route = route;
        this.stops = stops;
        trips = new ArrayList<TripsModelInfo>();
        segments = new ArrayList<RouteSegment>();
        exemplarSegments = new ArrayList<RouteSegment>();
        interlines = new ArrayList<PatternInterlineDwell>();
        this.mode = GtfsLibrary.getTraverseMode(route);
    }

    public void addExemplarSegment(RouteSegment segment) {
        exemplarSegments.add(segment);
    }

    public void addSegment(RouteSegment segment) {
        segments.add(segment);
    }

    @JsonIgnore
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
            LOG.error("Failed to organize hops in route variant " + name);
        }
    }

    @JsonIgnore
    public List<RouteSegment> segmentsAfter(RouteSegment segment) {
        HashMap<Edge, RouteSegment> successors = new HashMap<Edge, RouteSegment>();
        for (RouteSegment s : segments) {
            if (s.hopIn != null) {
                successors.put(s.hopIn, s);
            }
        }

        // skip this seg
        segment = successors.get(segment.hopOut);
        ArrayList<RouteSegment> out = new ArrayList<RouteSegment>();
        while (segment != null) {
            out.add(segment);
            segment = successors.get(segment.hopOut);
        }
        return out;
    }

    @XmlElementWrapper
    @XmlElement(name = "stop")
    @XmlJavaTypeAdapter(StopAgencyAndIdAdapter.class)
    public List<Stop> getStops() {
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

    @XmlElementWrapper
    @XmlElement(name = "trip")
    public List<TripsModelInfo> getTrips() {
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

    @JsonSerialize(using = EncodedPolylineJSONSerializer.class)
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
            geometry = GeometryUtils.getGeometryFactory().createLineString(
                    coords.toArray(coordArray));

        }
        return geometry;
    }
    
    /**
     * @param index The index of the segment in the list
     * @return The partial geometry between this segment's stop and the next one.
     */
    public LineString getGeometrySegment(int index) {
        RouteSegment segment = exemplarSegments.get(index);
        if (segment.hopOut != null) {
            return GeometryUtils.getGeometryFactory().createLineString(
                    segment.getGeometry().getCoordinates());
        }
        return null;
    }

    @JsonIgnore
    public TraverseMode getTraverseMode() {
        return mode;
    }

    public void setGeometry(LineString geometry) {
        this.geometry = geometry;
    }

    public void addInterline(PatternInterlineDwell dwell) {
        interlines.add(dwell);
    }

    @JsonIgnore
    public List<PatternInterlineDwell> getInterlines() {
        return interlines;
    }

    public void addTrip(Trip trip, int number) {
        this.trips.add(new TripsModelInfo(trip.getTripHeadsign(), number, trip.getServiceId()
                .getId(), trip.getId()));
        if (direction == null) {
            direction = trip.getDirectionId();
        } else {
            if (!direction.equals(trip.getDirectionId())) {
                direction = MULTIDIRECTION;
            }
        }
    }

}