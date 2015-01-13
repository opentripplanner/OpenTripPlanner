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

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.common.geometry.ReversibleLineStringWrapper;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * This index is used in Analyst and does not need to be instantiated if you are not performing
 * Analyst requests.
 */
public class GeometryIndex implements GeometryIndexService {
    
    private static final Logger LOG = LoggerFactory.getLogger(GeometryIndex.class);
    private static final double SEARCH_RADIUS_M = 100; // meters
    private static final double SEARCH_RADIUS_DEG = SphericalDistanceLibrary.metersToDegrees(SEARCH_RADIUS_M);

    GraphService graphService;
    
    private STRtree pedestrianIndex;

    public GeometryIndex(Graph graph) {
        if (graph == null) { 
            String message = "Could not retrieve default Graph from GraphService. Check its configuration.";
            LOG.error(message);
            throw new IllegalStateException(message);
        }
        Map<ReversibleLineStringWrapper, StreetEdge> edges = Maps.newHashMap();
        for (StreetVertex vertex : Iterables.filter(graph.getVertices(), StreetVertex.class)) {
            for (StreetEdge e: Iterables.filter(vertex.getOutgoing(), StreetEdge.class)) {
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
    
}
