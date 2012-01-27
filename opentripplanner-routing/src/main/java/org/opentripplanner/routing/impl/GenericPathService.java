package org.opentripplanner.routing.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Coordinate;

public abstract class GenericPathService implements PathService {

    private static final String _doublePattern = "-{0,1}\\d+(\\.\\d+){0,1}";

    private static final Pattern _latLonPattern = Pattern.compile("^\\s*(" + _doublePattern
            + ")(\\s*,\\s*|\\s+)(" + _doublePattern + ")\\s*$");

    protected StreetVertexIndexService _indexService;
    protected GraphService _graphService;


    @Autowired
    public void setGraphService(GraphService graphService) {
        _graphService = graphService;
    }

    @Override
    public GraphService getGraphService() {
        return _graphService;
    }

    @Autowired
    public void setIndexService(StreetVertexIndexService indexService) {
        _indexService = indexService;
    }

    protected Vertex getVertexForPlace(NamedPlace place, TraverseOptions options) {
        return getVertexForPlace(place, options, null);
    }

    protected Vertex getVertexForPlace(NamedPlace place, TraverseOptions options, Vertex other) {

        Matcher matcher = _latLonPattern.matcher(place.place);

        if (matcher.matches()) {
            double lat = Double.parseDouble(matcher.group(1));
            double lon = Double.parseDouble(matcher.group(4));
            Coordinate location = new Coordinate(lon, lat);
            if (other instanceof StreetLocation) {
                return _indexService.getClosestVertex(location, place.name, options, ((StreetLocation) other).getExtra());
            } else {
                return _indexService.getClosestVertex(location, place.name, options);
            }
        }
        throw new UnsupportedOperationException("latlon not matched, vertex label lookup deprecated");
        //return _graphService.getGraph().getVertex(place.place);
    }

    @Override
    public boolean isAccessible(NamedPlace place, TraverseOptions options) {
        /* fixme: take into account slope for wheelchair accessibility */
        Vertex vertex = getVertexForPlace(place, options);
        if (vertex instanceof TransitStop) {
            TransitStop ts = (TransitStop) vertex;
            return ts.hasWheelchairEntrance();
        } else if (vertex instanceof StreetLocation) {
            StreetLocation sl = (StreetLocation) vertex;
            return sl.isWheelchairAccessible();
        }
        return true;
    }
}
