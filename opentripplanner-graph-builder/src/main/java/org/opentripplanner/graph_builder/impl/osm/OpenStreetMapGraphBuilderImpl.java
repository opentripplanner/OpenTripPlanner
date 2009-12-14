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

package org.opentripplanner.graph_builder.impl.osm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.PackedCoordinateSequence.Float;
import org.opentripplanner.graph_builder.model.osm.OSMNode;
import org.opentripplanner.graph_builder.model.osm.OSMRelation;
import org.opentripplanner.graph_builder.model.osm.OSMWay;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.vertextypes.Intersection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class OpenStreetMapGraphBuilderImpl implements GraphBuilder {
    
    private static Logger _log = LoggerFactory.getLogger(OpenStreetMapGraphBuilderImpl.class);

    private static final GeometryFactory _geometryFactory = new GeometryFactory();
    
    private List<OpenStreetMapProvider> _providers = new ArrayList<OpenStreetMapProvider>();
    
    private Map<Object,Object> _uniques = new HashMap<Object, Object>();

    public void setProvider(OpenStreetMapProvider provider) {
        _providers.add(provider);
    }

    public void setProviders(List<OpenStreetMapProvider> providers) {
        _providers.addAll(providers);
    }

    @Override
    public void buildGraph(Graph graph) {
        Handler handler = new Handler();
        for (OpenStreetMapProvider provider : _providers) {
            _log.debug("gathering osm from provider: " + provider);
            provider.readOSM(handler);
        }
        _log.debug("building osm street graph");
        handler.buildGraph(graph);
    }
    
    @SuppressWarnings("unchecked")
    private <T> T unique(T value) {
        Object v = _uniques.get(value);
        if( v == null) {
            _uniques.put(value,value);
            v = value;
        }
        return (T) v;
    }

    private class Handler implements OpenStreetMapContentHandler {

        private Map<Integer, OSMNode> _nodes = new HashMap<Integer, OSMNode>();

        private Map<Integer, OSMWay> _ways = new HashMap<Integer, OSMWay>();

        public void buildGraph(Graph graph) {

            // We want to prune nodes that don't have any edges
            Set<Integer> nodesWithNeighbors = new HashSet<Integer>();

            for (OSMWay way : _ways.values()) {
                List<Integer> nodes = way.getNodeRefs();
                if (nodes.size() > 1)
                    nodesWithNeighbors.addAll(nodes);
            }

            // Remove all island
            _nodes.keySet().retainAll(nodesWithNeighbors);
            
            int nodeIndex = 0;
            
            for (OSMNode node : _nodes.values()) {
                
                if( nodeIndex % 1000 == 0)
                    _log.debug("nodes=" + nodeIndex + "/" + _nodes.size());
                nodeIndex++;
                
                int nodeId = node.getId();
                String id = getVertexIdForNodeId(nodeId);
                Vertex vertex = graph.getVertex(id);

                if (vertex != null)
                    throw new IllegalStateException("osm node already loaded: id=" + id);

                GenericVertex newVertex = new GenericVertex(id, node.getLon(), node
                        .getLat());
                newVertex.setType(Intersection.class);
                graph.addVertex(newVertex);

            }

            int wayIndex = 0;
            
            for (OSMWay way : _ways.values()) {
                
                if( wayIndex % 1000 == 0)
                    _log.debug("ways=" + wayIndex + "/" + _ways.size());
                wayIndex++;
                
                StreetTraversalPermission permissions = getPermissionsForWay(way);
                List<Integer> nodes = way.getNodeRefs();
                for (int i = 0; i < nodes.size() - 1; i++) {
                    String vFromId = getVertexIdForNodeId(nodes.get(i));
                    String vToId = getVertexIdForNodeId(nodes.get(i + 1));
                    Vertex from = graph.getVertex(vFromId);
                    Vertex to = graph.getVertex(vToId);
                    if (from == null || to == null)
                        continue;
                    double d = from.distance(to);
                    graph.addEdge(getEdgeForStreet(from, to, way, d, permissions));
                    graph.addEdge(getEdgeForStreet(to, from, way, d, permissions));
                }
            }

        }

        public void addNode(OSMNode node) {

            if (_nodes.containsKey(node.getId()))
                return;

            _nodes.put(node.getId(), node);
            
            if( _nodes.size() % 1000 == 0)
                _log.debug("nodes=" + _nodes.size());
        }

        public void addWay(OSMWay way) {
            if (_ways.containsKey(way.getId()))
                return;
            _ways.put(way.getId(), way);
            
            if( _ways.size() % 1000 == 0)
                _log.debug("ways=" + _ways.size());
        }

        public void addRelation(OSMRelation relation) {

        }

        private String getVertexIdForNodeId(int nodeId) {
            return "osm node " + nodeId;
        }

        private Street getEdgeForStreet(Vertex from, Vertex to, OSMWay way, double d,
                StreetTraversalPermission permissions) {
            
            String id = "way " + way.getId();
            
            id = unique(id);
            
            String name = way.getTags().get("name");
            if (name == null) {
                name = id;
            }
            Street street = new Street(from, to, id, name, d, permissions);
            
            Coordinate[] coordinates = { from.getCoordinate(), to.getCoordinate()};
            Float sequence = new PackedCoordinateSequence.Float(coordinates, 2);
            LineString lineString = _geometryFactory.createLineString(sequence);
            
            street.setGeometry(lineString);
            
            return street;
        }

        private StreetTraversalPermission getPermissionsForWay(OSMWay way) {

            // TODO : Better mapping between OSM tags and travel permissions

            Map<String, String> tags = way.getTags();
            String value = tags.get("highway");

            if (value == null || value.equals("motorway") || value.equals("motorway_link"))
                return StreetTraversalPermission.CAR_ONLY;

            return StreetTraversalPermission.ALL;
        }
    }
}
