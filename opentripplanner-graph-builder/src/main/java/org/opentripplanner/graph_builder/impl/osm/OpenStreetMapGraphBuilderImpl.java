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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.PackedCoordinateSequence.Float;
import org.opentripplanner.graph_builder.model.osm.OSMWithTags;
import org.opentripplanner.graph_builder.model.osm.OSMNode;
import org.opentripplanner.graph_builder.model.osm.OSMRelation;
import org.opentripplanner.graph_builder.model.osm.OSMWay;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.StreetUtils;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Intersection;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class OpenStreetMapGraphBuilderImpl implements GraphBuilder {

    private static Logger _log = LoggerFactory.getLogger(OpenStreetMapGraphBuilderImpl.class);

    private static final GeometryFactory _geometryFactory = new GeometryFactory();

    private List<OpenStreetMapProvider> _providers = new ArrayList<OpenStreetMapProvider>();

    private Map<Object, Object> _uniques = new HashMap<Object, Object>();

    private Map<String, KeyValuePermission> _tagPermissions = new LinkedHashMap<String, KeyValuePermission>();

    private class KeyValuePermission {
        public String key;
        public String value;
        public StreetTraversalPermission permission;

        public KeyValuePermission(String key, String value, StreetTraversalPermission permission) {
            this.key        = key;
            this.value      = value;
            this.permission = permission;
        }
    };

    public void setProvider(OpenStreetMapProvider provider) {
        _providers.add(provider);
    }

    public void setProviders(List<OpenStreetMapProvider> providers) {
        _providers.addAll(providers);
    }

    public void setDefaultAccessPermissions(LinkedHashMap<String, StreetTraversalPermission> mappy) {
        for(String tag : mappy.keySet()) {
            int ch_eq = tag.indexOf("=");

            if(ch_eq < 0){
                _tagPermissions.put(tag, new KeyValuePermission(null, null, mappy.get(tag)));
            } else {
                String key   = tag.substring(0, ch_eq),
                       value = tag.substring(ch_eq + 1);

                _tagPermissions.put(tag, new KeyValuePermission(key, value, mappy.get(tag)));
            }
        }
        if (!_tagPermissions.containsKey("__default__")) {
            _log.warn("No default permissions for osm tags...");
        }
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
        if (v == null) {
            _uniques.put(value, value);
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

                if (wayIndex % 1000 == 0)
                    _log.debug("ways=" + wayIndex + "/" + _ways.size());
                wayIndex++;

                StreetTraversalPermission permissions = getPermissionsForEntity(way);
                if(permissions == StreetTraversalPermission.NONE)
                    continue;

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

                    ArrayList<Street> streets = getEdgesForStreet(from, to, way, permissions);
                    for(Street street : streets) {
                        graph.addEdge(street);
                        Vertex start = street.getFromVertex();

                        ArrayList<Edge> edges = edgesByLocation.get(start.getCoordinate());
                        if (edges == null) {
                            edges = new ArrayList<Edge>(4);
                            edgesByLocation.put(start.getCoordinate(), edges);
                        }
                        edges.add(street);
                    }
                }
            }

            StreetUtils.createTurnEdges(graph, edgesByLocation);

        }

        private Vertex addVertex(Graph graph, String vertexId, OSMNode node) {
            Intersection newVertex = new Intersection(vertexId, node.getLon(), node.getLat());
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

        private HashSet<Integer> computeConnectedSubgraph(
                Map<Integer, ArrayList<Integer>> neighborsForNode, int startNode) {
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

            if (_nodes.size() % 1000 == 0)
                _log.debug("nodes=" + _nodes.size());
        }

        public void addWay(OSMWay way) {
            if (_ways.containsKey(way.getId()))
                return;

            if (!(way.getTags().containsKey("highway") || "platform".equals(way.getTags().get(
                    "railway")))) {
                return;
            }

            _ways.put(way.getId(), way);

            if (_ways.size() % 1000 == 0)
                _log.debug("ways=" + _ways.size());
        }

        public void addRelation(OSMRelation relation) {

        }

        private String getVertexIdForNodeId(int nodeId) {
            return "osm node " + nodeId;
        }

        /* Attempt at handling oneway streets, cycleways, and whatnot. See
         * http://wiki.openstreetmap.org/wiki/Bicycle for various scenarios,
         * along with http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Oneway. */
        private ArrayList<Street> getEdgesForStreet(Vertex from, Vertex to, OSMWay way,
                StreetTraversalPermission permissions) {
            ArrayList<Street> streets = new ArrayList<Street>();
            double d = from.distance(to);

            Map<String, String> tags = way.getTags();

            if(permissions == StreetTraversalPermission.NONE)
                return streets;

            /* Three basic cases, 1) bidirectonal for everyone, 2) unidirctional for cars only,
             * 3) biderectional for pedestrians only. */
            if("yes".equals(tags.get("oneway")) &&
                    ("no".equals(tags.get("oneway:bicycle"))
                     || "opposite_lane".equals(tags.get("cycleway"))
                     || "opposite".equals(tags.get("cycleway")))) { // 2.
                streets.add(getEdgeForStreet(from, to, way, d, permissions));
                if(permissions.remove(StreetTraversalPermission.CAR) != StreetTraversalPermission.NONE)
                    streets.add(getEdgeForStreet(to, from, way, d, permissions.remove(StreetTraversalPermission.CAR)));
            } else if ("yes".equals(tags.get("oneway")) || "roundabout".equals(tags.get("junction"))) { // 3
                streets.add(getEdgeForStreet(from, to, way, d, permissions));
                if(permissions.allows(StreetTraversalPermission.PEDESTRIAN))
                    streets.add(getEdgeForStreet(to, from, way, d, StreetTraversalPermission.PEDESTRIAN));
            } else { // 1.
                streets.add(getEdgeForStreet(from, to, way, d, permissions));
                streets.add(getEdgeForStreet(to, from, way, d, permissions));
            }


            return streets;
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

            /* TODO: This should probably generalized somehow? */
            if( "no".equals(way.getTags().get("wheelchair")) ||
               ("steps".equals(way.getTags().get("highway")) && !"yes".equals(way.getTags().get("wheelchair")))) {
                street.setWheelchairAccessible(false);
            }

            Coordinate[] coordinates = { from.getCoordinate(), to.getCoordinate() };
            Float sequence = new PackedCoordinateSequence.Float(coordinates, 2);
            LineString lineString = _geometryFactory.createLineString(sequence);

            street.setGeometry(lineString);

            return street;
        }

        private StreetTraversalPermission getPermissionsForEntity(OSMWithTags entity) {
            Map<String, String> tags = entity.getTags();
            StreetTraversalPermission def    = null;
            StreetTraversalPermission permission = null;

            String access = tags.get("access");
            String motorcar = tags.get("motorcar");
            String bicycle = tags.get("bicycle");
            String foot = tags.get("foot");
            String all_tags = access != null ? "access=" + access : "; ";
            all_tags += motorcar != null ? "motorcar=" + motorcar : "; ";
            all_tags += bicycle != null ? "bicycle=" + bicycle : "; ";
            all_tags += foot != null ? "foot=" + foot : "; ";

            for(KeyValuePermission kvp : _tagPermissions.values()) {
                if(tags.containsKey(kvp.key) && kvp.value.equals(tags.get(kvp.key))) {
                    def = kvp.permission;
                    break;
                }
            }

            if(def == null) {
                if(_tagPermissions.containsKey("__default__")) {
                    def = _tagPermissions.get("__default__").permission;
                    _log.debug("Used default permissions: " + all_tags);
                } else {
                    def = StreetTraversalPermission.ALL;
                }
            }

            /* Only access=*, motorcar=*, bicycle=*, and foot=* is examined,
             * since those are the only modes supported by OTP
             * (wheelchairs are not of concern here)
             *
             * Only *=no, and *=private are checked for, all other values are
             * presumed to be permissive (=> This may not be perfect, but is
             * closer to reality, since most people don't follow the rules
             * perfectly ;-)
             */
            if(access != null) {
                if("no".equals( access )) {
                    permission = StreetTraversalPermission.NONE;
                } else {
                    permission = StreetTraversalPermission.ALL;
                }
            } else if (motorcar != null || bicycle != null || foot != null) {
                permission = def;
            }

            if (motorcar != null) {
                if("no".equals(motorcar) || "private".equals(motorcar)) {
                    permission = permission.remove(StreetTraversalPermission.CAR);
                } else {
                    permission = permission.add(StreetTraversalPermission.CAR);
                }
            }

            if (tags.containsKey("bicycle")) {
                if("no".equals(bicycle) || "private".equals(bicycle)) {
                    permission = permission.remove(StreetTraversalPermission.BICYCLE);
                } else {
                    permission = permission.add(StreetTraversalPermission.BICYCLE);
                }
            }

            if (foot != null) {
                if("no".equals(foot) || "private".equals(foot)) {
                    permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
                } else {
                    permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
                }
            }


            if(permission == null)
                return def;

            return permission;
        }
    }
}
