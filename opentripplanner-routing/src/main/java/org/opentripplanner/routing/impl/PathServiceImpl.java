package org.opentripplanner.routing.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.services.calendar.CalendarServiceData;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
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

    private static final Pattern _latLonPattern = Pattern.compile("^(" + _doublePattern + ")(\\s*,\\s*|\\s+)("
            + _doublePattern + ")$");

    private Graph _graph;

    private RoutingService _routingService;

    private StreetVertexIndexService _indexService;

    private CalendarServiceImpl _calendarService = null;

    @Autowired
    public void setGraph(Graph graph) {
        _graph = graph;
        
        if( _graph.hasService(CalendarServiceData.class)) {
            CalendarServiceData data = _graph.getService(CalendarServiceData.class);
            CalendarServiceImpl calendarService = new CalendarServiceImpl();
            calendarService.setServiceCalendarData(data);
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
    public List<GraphPath> plan(String fromPlace, String toPlace, Date targetTime, boolean arriveBy) {

        Vertex fromVertex = getVertexForPlace(fromPlace);
        Vertex toVertex = getVertexForPlace(toPlace);

        State state = new State(targetTime.getTime());
        TraverseOptions options = new TraverseOptions();
        if( _calendarService != null)
            options.setCalendarService(_calendarService);
        
        options.back = arriveBy;

        GraphPath path = _routingService.route(fromVertex, toVertex, state, options);

        return Arrays.asList(path);
    }

    private Vertex getVertexForPlace(String place) {
        
        Matcher matcher = _latLonPattern.matcher(place);
        
        if( matcher.matches() ) {
            double lat = Double.parseDouble(matcher.group(1));
            double lon = Double.parseDouble(matcher.group(4));
            Coordinate location = new Coordinate(lon, lat);
            return _indexService.getClosestVertex(location);
        }
        
        return _graph.getVertex(place);
    }

}
