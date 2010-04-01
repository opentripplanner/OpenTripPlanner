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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.GenericStreetIntersectionVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.OneStreetVertex;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.RoutingService;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.spt.GraphPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;

@Component
public class PathServiceImpl implements PathService {

    private static final String _doublePattern = "-{0,1}\\d+(\\.\\d+){0,1}";

    private static final Pattern _latLonPattern = Pattern.compile("^\\s*(" + _doublePattern
            + ")(\\s*,\\s*|\\s+)(" + _doublePattern + ")\\s*$");

    private Graph _graph;

    private RoutingService _routingService;

    private StreetVertexIndexService _indexService;

    private CalendarServiceImpl _calendarService = null;

    @Autowired
    public void setGraph(Graph graph) {
        _graph = graph;

        if (_graph.hasService(CalendarServiceData.class)) {
            CalendarServiceData data = _graph.getService(CalendarServiceData.class);
            CalendarServiceImpl calendarService = new CalendarServiceImpl();
            calendarService.setData(data);
            _calendarService = calendarService;
        }
    }

    @Autowired
    public void setRoutingService(RoutingService routingService) {
        _routingService = routingService;
    }

    @Autowired
    public void setIndexService(StreetVertexIndexService indexService) {
        _indexService = indexService;
    }

    @Override
    public List<GraphPath> plan(String fromPlace, String toPlace, Date targetTime,
            TraverseOptions options) {

        ArrayList<String> notFound = new ArrayList<String>();
        Vertex fromVertex = getVertexForPlace(fromPlace);
        if (fromVertex == null) {
            notFound.add("from");
        }
        Vertex toVertex = getVertexForPlace(toPlace);
        if (toVertex == null) {
            notFound.add("to");
        }

        if (notFound.size() > 0) {
            throw new VertexNotFoundException(notFound);
        }

        State state = new State(targetTime.getTime());

        if (_calendarService != null)
            options.setCalendarService(_calendarService);

        GraphPath path = _routingService.route(fromVertex, toVertex, state, options);
        if (path == null) {
            return null;
        }
        return Arrays.asList(path);
    }

    @Override
    public List<GraphPath> plan(String fromPlace, String toPlace, List<String> intermediates,
            Date targetTime, TraverseOptions options) {

        ArrayList<String> notFound = new ArrayList<String>();
        Vertex fromVertex = getVertexForPlace(fromPlace);
        if (fromVertex == null) {
            notFound.add("from");
        }
        Vertex toVertex = getVertexForPlace(toPlace);
        if (toVertex == null) {
            notFound.add("to");
        }
        ArrayList<Vertex> intermediateVertices = new ArrayList<Vertex>();

        int i = 0;
        for (String intermediate : intermediates) {
            Vertex vertex = getVertexForPlace(intermediate);
            if (vertex == null) {
                notFound.add("intermediate." + i);
            } else {
                intermediateVertices.add(vertex);
            }
            i += 1;
        }

        if (notFound.size() > 0) {
            throw new VertexNotFoundException(notFound);
        }

        State state = new State(targetTime.getTime());

        if (_calendarService != null)
            options.setCalendarService(_calendarService);

        GraphPath path = _routingService.route(fromVertex, toVertex, intermediateVertices, state, options);

        return Arrays.asList(path);
    }

    private Vertex getVertexForPlace(String place) {

        Matcher matcher = _latLonPattern.matcher(place);

        if (matcher.matches()) {
            double lat = Double.parseDouble(matcher.group(1));
            double lon = Double.parseDouble(matcher.group(4));
            Coordinate location = new Coordinate(lon, lat);
            return _indexService.getClosestVertex(location);
        }

        return _graph.getVertex(place);
    }

    @Override
    public boolean isAccessible(String place, double maxSlope) {
        Vertex vertex = getVertexForPlace(place);
        if (vertex instanceof TransitStop) {
            TransitStop ts = (TransitStop) vertex;
            return ts.hasWheelchairEntrance();
        } else if (vertex instanceof GenericStreetIntersectionVertex) {
            for (Edge e : vertex.getIncoming()) {
                if (e instanceof Street) {
                    Street s = (Street) e;
                    if (s.getWheelchairAccessible(maxSlope)) {
                        return true;
                    }
                }
            }
            return false;
        } else if (vertex instanceof StreetLocation) {
            StreetLocation sl = (StreetLocation) vertex;
            for (Street street : sl.streets) {
                if (street.getWheelchairAccessible(maxSlope)) {
                    return true;
                }
            }
            return false;
        } else if (vertex instanceof OneStreetVertex) {
            OneStreetVertex osv = (OneStreetVertex) vertex;
            return osv.outStreet.getWheelchairAccessible(maxSlope);
        }
        return true;
    }

}
