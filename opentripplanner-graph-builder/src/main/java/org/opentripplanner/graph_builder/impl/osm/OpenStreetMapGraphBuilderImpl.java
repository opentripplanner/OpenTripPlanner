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

import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.model.osm.OSMWithTags;
import org.opentripplanner.graph_builder.model.osm.OSMNode;
import org.opentripplanner.graph_builder.model.osm.OSMRelation;
import org.opentripplanner.graph_builder.model.osm.OSMWay;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.StreetUtils;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.EndpointVertex;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Builds a street graph from OpenStreetMap data.
 * 
 */
public class OpenStreetMapGraphBuilderImpl implements GraphBuilder {

    private static Logger _log = LoggerFactory.getLogger(OpenStreetMapGraphBuilderImpl.class);

    private List<OpenStreetMapProvider> _providers = new ArrayList<OpenStreetMapProvider>();

    private Map<Object, Object> _uniques = new HashMap<Object, Object>();

    private Map<String, KeyValuePermission> _tagPermissions = new LinkedHashMap<String, KeyValuePermission>();

    private HashMap<P2<String>, P2<Double>> safetyFeatures = new HashMap<P2<String>, P2<Double>>();

    private HashSet<P2<String>> _slopeOverrideTags = new HashSet<P2<String>>();

    private class KeyValuePermission {
        public String key;

        public String value;

        public StreetTraversalPermission permission;

        public KeyValuePermission(String key, String value, StreetTraversalPermission permission) {
            this.key = key;
            this.value = value;
            this.permission = permission;
        }
    };

    /**
     * The source for OSM map data
     */
    public void setProvider(OpenStreetMapProvider provider) {
        _providers.add(provider);
    }

    /**
     * Multiple sources for OSM map data
     */
    public void setProviders(List<OpenStreetMapProvider> providers) {
        _providers.addAll(providers);
    }

    /**
     * The set of traversal permissions for a given set of tags.
     * 
     * @param provider
     */
    public void setDefaultAccessPermissions(LinkedHashMap<String, StreetTraversalPermission> mappy) {
        for (String tag : mappy.keySet()) {
            int ch_eq = tag.indexOf("=");

            if (ch_eq < 0) {
                _tagPermissions.put(tag, new KeyValuePermission(null, null, mappy.get(tag)));
            } else {
                String key = tag.substring(0, ch_eq), value = tag.substring(ch_eq + 1);

                _tagPermissions.put(tag, new KeyValuePermission(key, value, mappy.get(tag)));
            }
        }
        if (!_tagPermissions.containsKey("__default__")) {
            _log.warn("No default permissions for osm tags...");
        }
    }

    /**
     * Streets where the slope is assumed to be flat because the underlying topographic
     * data cannot be trusted
     * 
     * @param features a list of osm attributes in the form key=value
     */
    public void setSlopeOverride(List<String> features) {
        for (String tag : features) {
            int ch_eq = tag.indexOf("=");

            if (ch_eq >= 0) {
                String key = tag.substring(0, ch_eq), value = tag.substring(ch_eq + 1);
                _slopeOverrideTags.add(new P2<String>(key, value));
            } 
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

    /**
     * Sets processing of bicycle safety features from OSM tags. Takes a map from key,value pairs to
     * forwards,backwards multipliers. In Spring XML, this looks like:
     * 
     * <property name="safetyFeatures"> 
     *   <map> 
     *     <entry key="opposite_lane=cycleway" value="1,0.1" />
     *     <entry key="this_lane=cycleway" value="0.1,1" />
     *    </map>
     *  </property>
     *
     * Entries are multiplied 
     *
     * @param features
     */
    public void setSafetyFeatures(Map<String, String> features) {
        safetyFeatures = new HashMap<P2<String>, P2<Double>>();
        for (Map.Entry<String, String> entry : features.entrySet()) {
            String[] kv = entry.getKey().split("=");
            String[] strings = entry.getValue().split(",");
            P2<Double> values = new P2<Double>(Double.parseDouble(strings[0]), Double
                    .parseDouble(strings[1]));
            safetyFeatures.put(new P2<String>(kv), values);
        }
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

            int wayIndex = 0;

            // figure out which nodes that are actually intersections
            Set<Integer> possibleIntersectionNodes = new HashSet<Integer>();
            Set<Integer> intersectionNodes = new HashSet<Integer>();
            for (OSMWay way : _ways.values()) {
                List<Integer> nodes = way.getNodeRefs();
                for (int node : nodes) {
                    if (possibleIntersectionNodes.contains(node)) {
                        intersectionNodes.add(node);
                    } else {
                        possibleIntersectionNodes.add(node);
                    }
                }
            }
            GeometryFactory geometryFactory = new GeometryFactory();
            
            /* build an ordinary graph, which we will convert to an edge-based graph */
            ArrayList<Vertex> endpoints = new ArrayList<Vertex>();

            for (OSMWay way : _ways.values()) {

                if (wayIndex % 1000 == 0)
                    _log.debug("ways=" + wayIndex + "/" + _ways.size());
                wayIndex++;
                StreetTraversalPermission permissions = getPermissionsForEntity(way);
                if (permissions == StreetTraversalPermission.NONE)
                    continue;

                List<Integer> nodes = way.getNodeRefs();

                Vertex startEndpoint = null, endEndpoint = null;

                ArrayList<Coordinate> segmentCoordinates = new ArrayList<Coordinate>();

                /*
                 * Traverse through all the nodes of this edge. For nodes which are not shared with
                 * any other edge, do not create endpoints -- just accumulate them for geometry. For
                 * nodes which are shared, create endpoints and StreetVertex instances.
                 */

                Integer startNode = null;
                OSMNode osmStartNode = null;
                for (int i = 0; i < nodes.size() - 1; i++) {
                    Integer endNode = nodes.get(i + 1);
                    if (osmStartNode == null) {
                        startNode = nodes.get(i);
                        osmStartNode = _nodes.get(startNode);
                    }
                    OSMNode osmEndNode = _nodes.get(endNode);

                    if (osmStartNode == null || osmEndNode == null)
                        continue;

                    LineString geometry;

                    /*
                     * skip vertices that are not intersections, except that we use them for
                     * geometry
                     */
                    if (segmentCoordinates.size() == 0) {
                        segmentCoordinates.add(getCoordinate(osmStartNode));
                    }

                    if (intersectionNodes.contains(endNode) || i == nodes.size() - 2) {
                        segmentCoordinates.add(getCoordinate(osmEndNode));
                        geometry = geometryFactory.createLineString(segmentCoordinates
                                .toArray(new Coordinate[0]));
                        segmentCoordinates.clear();
                    } else {
                        segmentCoordinates.add(getCoordinate(osmEndNode));
                        continue;
                    }

                    /* generate endpoints */
                    if (startEndpoint == null) {
                        //first iteration on this way
                        String label = "osm node " + osmStartNode.getId();

                        startEndpoint = graph.getVertex(label);
                        if (startEndpoint == null) {
                            Coordinate coordinate = getCoordinate(osmStartNode);
                            startEndpoint = new EndpointVertex(label, coordinate.x, coordinate.y,
                                    label);
                            graph.addVertex(startEndpoint);
                            endpoints.add(startEndpoint);
                        }
                    } else {
                        startEndpoint = endEndpoint;
                    }

                    String label = "osm node " + osmEndNode.getId();
                    endEndpoint = graph.getVertex(label);
                    if (endEndpoint == null) {
                        Coordinate coordinate = getCoordinate(osmEndNode);
                        endEndpoint = new EndpointVertex(label, coordinate.x, coordinate.y, label);
                        graph.addVertex(endEndpoint);
                        endpoints.add(endEndpoint);
                    }

                    P2<PlainStreetEdge> streets = getEdgesForStreet(startEndpoint, endEndpoint,
                            way, i, permissions, geometry);
                    PlainStreetEdge street = streets.getFirst();

                    if (street != null) {
                        graph.addEdge(street);
                    }

                    PlainStreetEdge backStreet = streets.getSecond();
                    if (backStreet != null) {
                        graph.addEdge(backStreet);
                    }

                    startNode = endNode;
                    osmStartNode = _nodes.get(startNode);
                }
            }

            StreetUtils.makeEdgeBased(graph, endpoints);
            
        }

        private Coordinate getCoordinate(OSMNode osmNode) {
            return new Coordinate(osmNode.getLon(), osmNode.getLat());
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
            /* remove all tiny subgraphs */
            for (HashSet<Integer> subgraph : subgraphs.values()) {
                if (subgraph.size() < 20) {
                    _nodes.keySet().removeAll(subgraph);
                }
            }
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

            if (_nodes.size() % 10000 == 0)
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

        /**
         * Handle oneway streets, cycleways, and whatnot. See
         * http://wiki.openstreetmap.org/wiki/Bicycle for various scenarios, along with
         * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Oneway.
         * 
         * @param end
         * @param start
         */
        private P2<PlainStreetEdge> getEdgesForStreet(Vertex start, Vertex end, OSMWay way,
                int startNode, StreetTraversalPermission permissions, LineString geometry) {
            // get geometry length in meters, irritatingly.
            Coordinate[] coordinates = geometry.getCoordinates();
            double d = 0;
            for (int i = 1; i < coordinates.length; ++i) {
                d += DistanceLibrary.distance(coordinates[i - 1], coordinates[i]);
            }

            
            LineString backGeometry = (LineString) geometry.reverse();

            Map<String, String> tags = way.getTags();

            if (permissions == StreetTraversalPermission.NONE)
                return new P2<PlainStreetEdge>(null, null);

            PlainStreetEdge street = null, backStreet = null;

            /*
             * Three basic cases, 1) bidirectional for everyone, 2) unidirectional for cars only, 3)
             * bidirectional for pedestrians only.
             */

            if ("yes".equals(tags.get("oneway"))
                    && ("no".equals(tags.get("oneway:bicycle"))
                            || "opposite_lane".equals(tags.get("cycleway")) || "opposite"
                            .equals(tags.get("cycleway")))) { // 2.
                street = getEdgeForStreet(start, end, way, startNode, d, permissions, geometry,
                        false);
                if (permissions.remove(StreetTraversalPermission.CAR) != StreetTraversalPermission.NONE)
                    backStreet = getEdgeForStreet(end, start, way, startNode, d, permissions
                            .remove(StreetTraversalPermission.CAR), backGeometry, true);
            } else if ("yes".equals(tags.get("oneway"))
                    || "roundabout".equals(tags.get("junction"))) { // 3
                street = getEdgeForStreet(start, end, way, startNode, d, permissions, geometry,
                        false);
                if (permissions.allows(StreetTraversalPermission.PEDESTRIAN))
                    backStreet = getEdgeForStreet(end, start, way, startNode, d,
                            StreetTraversalPermission.PEDESTRIAN, backGeometry, true);
            } else { // 1.
                street = getEdgeForStreet(start, end, way, startNode, d, permissions, geometry,
                        false);
                backStreet = getEdgeForStreet(end, start, way, startNode, d, permissions,
                        backGeometry, true);
            }

            /* set bicycle safety features according to configuration */

            for (Map.Entry<P2<String>, P2<Double>> feature : safetyFeatures.entrySet()) {
                String key = feature.getKey().getFirst();
                String value = feature.getKey().getSecond();
                if (value.equals(tags.get(key))) {
                    P2<Double> multipliers = feature.getValue();
                    if (street != null) {
                        street.setBicycleSafetyEffectiveLength(street.getBicycleSafetyEffectiveLength() * multipliers.getFirst());
                    }
                    if (backStreet != null) {
                        backStreet.setBicycleSafetyEffectiveLength(backStreet.getBicycleSafetyEffectiveLength() * multipliers.getSecond());
                    }
                }
            }
            return new P2<PlainStreetEdge>(street, backStreet);
        }

        private PlainStreetEdge getEdgeForStreet(Vertex start, Vertex end, OSMWay way,
                int startNode, double length, StreetTraversalPermission permissions,
                LineString geometry, boolean back) {

            String id = "way " + way.getId() + " from " + startNode;
            id = unique(id);

            String name = way.getTags().get("name");
            if (name == null) {
                name = id;
            }
            PlainStreetEdge street = new PlainStreetEdge(start, end, geometry, name, length,
                    permissions, back);
            street.setId(id);
            /* TODO: This should probably generalized somehow? */
            if ("no".equals(way.getTags().get("wheelchair"))
                    || ("steps".equals(way.getTags().get("highway")) && !"yes".equals(way.getTags()
                            .get("wheelchair")))) {
                street.setWheelchairAccessible(false);
            }
            
            Map<String, String> tags = way.getTags();
            for (P2<String> kvp : _slopeOverrideTags) {
                String key = kvp.getFirst();
                String value = kvp.getSecond();
                if (value.equals(tags.get(key))) {
                    street.setSlopeOverride(true);
                    break;
                }
            }

            return street;
        }

        private StreetTraversalPermission getPermissionsForEntity(OSMWithTags entity) {
            Map<String, String> tags = entity.getTags();
            StreetTraversalPermission def = null;
            StreetTraversalPermission permission = null;

            String access = tags.get("access");
            String motorcar = tags.get("motorcar");
            String bicycle = tags.get("bicycle");
            String foot = tags.get("foot");

            for (KeyValuePermission kvp : _tagPermissions.values()) {
                if (tags.containsKey(kvp.key) && kvp.value.equals(tags.get(kvp.key))) {
                    def = kvp.permission;
                    break;
                }
            }

            if (def == null) {
                if (_tagPermissions.containsKey("__default__")) {
                    String all_tags = null;
                    for (String key : tags.keySet()) {
                        String tag = key + "=" + tags.get(key);
                        if (all_tags == null) {
                            all_tags = tag;
                        } else {
                            all_tags += "; " + tag;
                        }
                    }
                    _log.debug("Used default permissions: " + all_tags);
                    def = _tagPermissions.get("__default__").permission;
                } else {
                    def = StreetTraversalPermission.ALL;
                }
            }

            /*
             * Only access=*, motorcar=*, bicycle=*, and foot=* is examined, since those are the
             * only modes supported by OTP (wheelchairs are not of concern here)
             * 
             * Only *=no, and *=private are checked for, all other values are presumed to be
             * permissive (=> This may not be perfect, but is closer to reality, since most people
             * don't follow the rules perfectly ;-)
             */
            if (access != null) {
                if ("no".equals(access) || "private".equals(access)) {
                    permission = StreetTraversalPermission.NONE;
                } else {
                    permission = StreetTraversalPermission.ALL;
                }
            } else if (motorcar != null || bicycle != null || foot != null) {
                permission = def;
            }

            if (motorcar != null) {
                if ("no".equals(motorcar) || "private".equals(motorcar)) {
                    permission = permission.remove(StreetTraversalPermission.CAR);
                } else {
                    permission = permission.add(StreetTraversalPermission.CAR);
                }
            }

            if (bicycle != null) {
                if ("no".equals(bicycle) || "private".equals(bicycle)) {
                    permission = permission.remove(StreetTraversalPermission.BICYCLE);
                } else {
                    permission = permission.add(StreetTraversalPermission.BICYCLE);
                }
            }

            if (foot != null) {
                if ("no".equals(foot) || "private".equals(foot)) {
                    permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
                } else {
                    permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
                }
            }

            if (permission == null)
                return def;

            return permission;
        }
    }
}
