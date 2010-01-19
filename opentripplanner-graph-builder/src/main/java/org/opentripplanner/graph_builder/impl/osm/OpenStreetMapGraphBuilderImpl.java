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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.PackedCoordinateSequence.Float;
import org.opentripplanner.graph_builder.model.osm.OSMNode;
import org.opentripplanner.graph_builder.model.osm.OSMRelation;
import org.opentripplanner.graph_builder.model.osm.OSMWay;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Intersection;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.Turn;
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

            // Remove all simple islands
            _nodes.keySet().retainAll(nodesWithNeighbors);
            
            pruneFloatingIslands();

            HashMap<Coordinate, ArrayList<Edge>> edgesByLocation = new HashMap<Coordinate, ArrayList<Edge>>();

            int wayIndex = 0;
            
            for (OSMWay way : _ways.values()) {
                
                if( wayIndex % 1000 == 0)
                    _log.debug("ways=" + wayIndex + "/" + _ways.size());
                wayIndex++;
                
                StreetTraversalPermission permissions = getPermissionsForWay(way);
                List<Integer> nodes = way.getNodeRefs();
                for (int i = 0; i < nodes.size() - 1; i++) {
                    Integer startNode = nodes.get(i);
                    String vFromId = getVertexIdForNodeId(startNode) + "_" + i + "_" + way.getId();
                    Integer endNode = nodes.get(i + 1);
                    String vToId = getVertexIdForNodeId(endNode) + "_" + i + "_" + way.getId();

                    OSMNode osmStartNode = _nodes.get(startNode);
                    OSMNode osmEndNode = _nodes.get(endNode);

                    if (osmStartNode == null || osmEndNode == null)
                        continue;
                    Vertex from = addVertex(graph, vFromId, osmStartNode);
                    Vertex to = addVertex(graph, vToId, osmEndNode);

                    double d = from.distance(to);
                    Street street = getEdgeForStreet(from, to, way, d, permissions);
                    graph.addEdge(street);
                    Street backStreet = getEdgeForStreet(to, from, way, d, permissions);
                    graph.addEdge(backStreet);

                    ArrayList<Edge> startEdges = edgesByLocation.get(from.getCoordinate());
                    if (startEdges == null) {
                        startEdges = new ArrayList<Edge>();
                        edgesByLocation.put(from.getCoordinate(), startEdges);
                    }
                    startEdges.add(street);
                    startEdges.add(backStreet);

                    ArrayList<Edge> endEdges = edgesByLocation.get(to.getCoordinate());
                    if (endEdges == null) {
                        endEdges = new ArrayList<Edge>();
                        edgesByLocation.put(to.getCoordinate(), endEdges);
                    }
                    endEdges.add(street);
                    endEdges.add(backStreet);
                }
            }

            //add turns
            for (ArrayList<Edge> edges : edgesByLocation.values()) {
                for (Edge in : edges) {
                    Vertex tov = in.getToVertex();
                    Coordinate c = tov.getCoordinate();
                    ArrayList<Edge> outEdges = edgesByLocation.get(c);
                    if (outEdges != null) {
                        for (Edge out : outEdges) {
                            /* Only create a turn edge if:
                             * (a) the edge is not the one we are coming from
                             * (b) the edge is a Street
                             * (c) the edge is an outgoing edge from this location
                             */
                            if (tov != out.getFromVertex() && out instanceof Street && out.getFromVertex().getCoordinate().equals(c)) {
                                graph.addEdge(new Turn(in, out));
                            }
                        }
                    }
                }
            }


        }

        private Vertex addVertex(Graph graph, String vertexId, OSMNode node) {
            Intersection newVertex = new Intersection(vertexId, node.getLon(), node
                    .getLat());
            graph.addVertex(newVertex);
            return newVertex;
        }

        private void pruneFloatingIslands() {
            Map<Integer, HashSet<Integer>> subgraphs = new HashMap<Integer, HashSet<Integer>>();
            Map<Integer, ArrayList<Integer>> neighborsForNode = new HashMap<Integer, ArrayList<Integer>>();
            for (OSMWay way : _ways.values()) {
                List<Integer> nodes = way.getNodeRefs();
                for (int node : nodes) {
                    ArrayList<Integer> nodelist = neighborsForNode.get(node);
                    if (nodelist == null) {
                        nodelist = new ArrayList<Integer>();
                        neighborsForNode.put(node, nodelist);                        
                    }
                    nodelist.addAll(nodes);
                }
            }
            /* associate each node with a subgraph */
            for (int node : _nodes.keySet()) {
                if (subgraphs.containsKey(node)) {
                    continue;
                }
                HashSet<Integer> subgraph = computeConnectedSubgraph(neighborsForNode, node);
                for (int subnode : subgraph) {
                    subgraphs.put(subnode, subgraph);                    
                }
            }        
            /* find the largest subgraph */
            HashSet<Integer> largestSubgraph = null;
            for (HashSet<Integer> subgraph : subgraphs.values()) {
                if (largestSubgraph == null || largestSubgraph.size() < subgraph.size()) {
                    largestSubgraph = subgraph;
                }
            }
            /* delete the rest */
            _nodes.keySet().retainAll(largestSubgraph);
        }
        
        private HashSet<Integer> computeConnectedSubgraph(Map<Integer, ArrayList<Integer>> neighborsForNode, int startNode) {
            HashSet<Integer> subgraph = new HashSet<Integer>();
            Queue<Integer> q = new LinkedList<Integer>();
            q.add(startNode);
            while (!q.isEmpty()) {
                int node = q.poll();
                for (int neighbor : neighborsForNode.get(node)) {
                    if (!subgraph.contains(neighbor)) {
                        subgraph.add(neighbor);
                        q.add(neighbor);
                    }
                }
            }
            return subgraph;
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

            if (!(way.getTags().containsKey("highway") || "platform".equals(way.getTags().get("railway")))) {
                return;
            }

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

            if ("platform".equals(way.getTags().get("railway"))) {
                return StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE_ONLY;
            }

            return StreetTraversalPermission.ALL;
        }
    }
}
