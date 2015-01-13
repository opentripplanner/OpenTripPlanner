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

package org.opentripplanner.analyst.batch;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class GraphGeographicFilter implements IndividualFilter {

    private static final Logger LOG = LoggerFactory.getLogger(GraphGeographicFilter.class);

    private RoutingRequest prototypeRoutingRequest;
    private GraphService graphService;

    public GraphGeographicFilter(RoutingRequest prototypeRoutingRequest, GraphService graphService) {
        this.prototypeRoutingRequest = prototypeRoutingRequest;
        this.graphService = graphService;
        findHull();
    }

    private double bufferMeters = 2000;
    private boolean useOnlyStops = true;
    private static GeometryFactory gf = new GeometryFactory();
    private Geometry hull;
    
    public void findHull() {
        LOG.info("finding hull of graph...");
        LOG.debug("using only stops? {}", useOnlyStops);
        if (bufferMeters < prototypeRoutingRequest.maxWalkDistance)
            LOG.warn("geographic filter buffer is smaller than max walk distance, this will probably yield incorrect results.");
        Graph graph= graphService.getRouter(prototypeRoutingRequest.routerId).graph;
        List<Geometry> geometries = new ArrayList<Geometry>();
        for (Vertex v : graph.getVertices()) {
            if (useOnlyStops && ! (v instanceof TransitStop))
                continue;
            Point pt = gf.createPoint(v.getCoordinate());
            Geometry geom = crudeProjectedBuffer(pt, bufferMeters);
            geometries.add(geom);
        }
        Geometry multiGeom = gf.buildGeometry(geometries);
        LOG.info("unioning hull...");
        hull = multiGeom.union();
        LOG.trace("hull is {}", hull.toText());
        // may lead to false rejections
        // DouglasPeuckerSimplifier simplifier = new DouglasPeuckerSimplifier();
    }
    
    private Geometry crudeProjectedBuffer(Point pt, double distanceMeters) {
        final double mPerDegreeLat = 111111.111111;
        double lat = pt.getY();
        double lonScale = Math.cos(Math.PI * lat / 180);
        double latExpand = distanceMeters / mPerDegreeLat;
        double lonExpand = latExpand / lonScale;
        Envelope env = pt.getEnvelopeInternal();
        env.expandBy(lonExpand, latExpand);
        return gf.toGeometry(env);
    }
    
    @Override
    public boolean filter(Individual individual) {
        Coordinate coord = new Coordinate(individual.lon, individual.lat);
        Point pt = gf.createPoint(coord);
        boolean accept = hull.contains(pt);
        //LOG.debug("label {} accept {}", individual.label, accept);
        return accept;
    }

}
