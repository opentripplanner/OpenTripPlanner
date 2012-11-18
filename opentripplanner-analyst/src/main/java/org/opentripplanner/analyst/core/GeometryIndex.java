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

package org.opentripplanner.analyst.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import lombok.Setter;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.strtree.STRtree;

@Component
public class GeometryIndex implements GeometryIndexService {
    
    private static final Logger LOG = LoggerFactory.getLogger(GeometryIndex.class);
    private static final double SEARCH_RADIUS_M = 100; // meters
    private static final double SEARCH_RADIUS_DEG = SphericalDistanceLibrary.metersToDegrees(SEARCH_RADIUS_M);
    
    @Autowired @Setter 
    GraphService graphService;
    
    private STRtree pedestrianIndex;
    
    @PostConstruct
    public void initialzeComponent() {
        Graph graph = graphService.getGraph();
        if (graph == null) { // analyst currently depends on there being a single default graph
            String message = "Could not retrieve default Graph from GraphService. Check its configuration.";
            LOG.error(message);
            throw new IllegalStateException(message);
        }
        Map<ReversibleLineStringWrapper, StreetEdge> edges = Maps.newHashMap();
        for (StreetVertex vertex : IterableLibrary.filter(graph.getVertices(), StreetVertex.class)) {
            for (StreetEdge e: IterableLibrary.filter(vertex.getOutgoing(), StreetEdge.class)) {
                LineString geom = e.getGeometry();
                if (e.getPermission().allows(StreetTraversalPermission.PEDESTRIAN)) {
                    edges.put(new ReversibleLineStringWrapper(geom), e);
                }
            }
        }
        // insert unique edges
        pedestrianIndex = new STRtree();
        for (StreetEdge e : edges.values()) {
            LineString geom = e.getGeometry();
            pedestrianIndex.insert(geom.getEnvelopeInternal(), e);
        }
        pedestrianIndex.build();
        LOG.debug("spatial index size: {}", pedestrianIndex.size());
    }
    
    @SuppressWarnings("rawtypes")
    public List queryPedestrian(Envelope env) {
        return pedestrianIndex.query(env);
    }
    
    @Override
    public BoundingBox getBoundingBox(CoordinateReferenceSystem crs) {
        try {
            Envelope bounds = (Envelope) pedestrianIndex.getRoot().getBounds();
            ReferencedEnvelope refEnv = new ReferencedEnvelope(bounds, CRS.decode("EPSG:4326", true));
            return refEnv.toBounds(crs);
        } catch (Exception e) {
            LOG.error("error transforming graph bounding box to request CRS : {}", crs);
            return null;
        }
    }
    
    class ReversibleLineStringWrapper {
        
        LineString ls;
        
        public ReversibleLineStringWrapper(LineString ls) {
            this.ls = ls;
        }
        
        /** Equality is defined as having the same number of coordinates with first and last 
         * coordinates matching either forward or in reverse. */
        @Override
        public boolean equals(Object other) {
            if ( ! (other instanceof ReversibleLineStringWrapper)) 
                return false;
            ReversibleLineStringWrapper that = (ReversibleLineStringWrapper) other;
            CoordinateSequence cs0 = ls.getCoordinateSequence();
            CoordinateSequence cs1 = that.ls.getCoordinateSequence();
            if (cs0.size() != cs1.size())
                return false;
            Coordinate c00 = cs0.getCoordinate(0);
            Coordinate c0n = cs0.getCoordinate(cs0.size() - 1);
            Coordinate c10 = cs1.getCoordinate(0);
            Coordinate c1n = cs1.getCoordinate(cs1.size() - 1);
            if (c00.equals(c10) && c0n.equals(c1n))
                return true;
            if (c00.equals(c1n) && c0n.equals(c10))
                return true;
            return false;
        }
        
        @Override
        public int hashCode() {
            CoordinateSequence cs = ls.getCoordinateSequence();
            int maxIdx = cs.size() - 1;
            int x = (int)(cs.getX(0) * 1000000) + (int)(cs.getX(maxIdx) * 1000000);
            int y = (int)(cs.getY(0) * 1000000) + (int)(cs.getY(maxIdx) * 1000000);
            return x + y * 101149 + maxIdx * 7883;
        }
        
    }
    
}
