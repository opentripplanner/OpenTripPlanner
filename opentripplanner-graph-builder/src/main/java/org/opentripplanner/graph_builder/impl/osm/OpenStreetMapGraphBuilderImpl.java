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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.common.StreetUtils;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.impl.extra_elevation_data.ElevationPoint;
import org.opentripplanner.graph_builder.impl.extra_elevation_data.ExtraElevationData;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMLevel.Source;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMRelation;
import org.opentripplanner.openstreetmap.model.OSMRelationMember;
import org.opentripplanner.openstreetmap.model.OSMTag;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.core.GraphBuilderAnnotation;
import org.opentripplanner.routing.core.GraphBuilderAnnotation.Variety;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.EdgeWithElevation;
import org.opentripplanner.routing.edgetype.ElevatorAlightEdge;
import org.opentripplanner.routing.edgetype.ElevatorBoardEdge;
import org.opentripplanner.routing.edgetype.ElevatorHopEdge;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.patch.TranslatedString;
import org.opentripplanner.routing.vertextype.ElevatorOffboardVertex;
import org.opentripplanner.routing.vertextype.ElevatorOnboardVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.util.MapUtils;
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

    private WayPropertySet wayPropertySet = new WayPropertySet();

    private CustomNamer customNamer;
    
    private ExtraElevationData extraElevationData = new ExtraElevationData();

    private boolean noZeroLevels = true;

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
     * Set the way properties from a {@link WayPropertySetSource} source.
     * 
     * @param source the way properties source
     */
    public void setDefaultWayPropertySetSource(WayPropertySetSource source) {
        wayPropertySet = source.getWayPropertySet();
    }

    /**
     * If true, disallow zero floors and add 1 to non-negative numeric floors, as is generally done
     * in the United States. This does not affect floor names from level maps. Default: true.
     */
    public void setNoZeroLevels(boolean nz) {
        noZeroLevels = nz;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        Handler handler = new Handler();
        for (OpenStreetMapProvider provider : _providers) {
            _log.debug("gathering osm from provider: " + provider);
            provider.readOSM(handler);
        }
        _log.debug("building osm street graph");
        handler.buildGraph(graph, extra);
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

    public void setWayPropertySet(WayPropertySet wayDataSet) {
        this.wayPropertySet = wayDataSet;
    }

    public WayPropertySet getWayPropertySet() {
        return wayPropertySet;
    }

    private class Handler implements OpenStreetMapContentHandler {

        private Map<Long, OSMNode> _nodes = new HashMap<Long, OSMNode>();
        private Map<Long, OSMWay> _ways = new HashMap<Long, OSMWay>();
        private Map<Long, OSMRelation> _relations = new HashMap<Long, OSMRelation>();
        private Set<Long> _nodesWithNeighbors = new HashSet<Long>();

        private Map<Long, List<TurnRestrictionTag>> turnRestrictionsByFromWay =
                new HashMap<Long, List<TurnRestrictionTag>>();

        private Map<Long, List<TurnRestrictionTag>> turnRestrictionsByToWay =
                new HashMap<Long, List<TurnRestrictionTag>>();

        private Map<TurnRestrictionTag, TurnRestriction> turnRestrictionsByTag =
                new HashMap<TurnRestrictionTag, TurnRestriction>();

        private Graph graph;

        /** The bike safety factor of the safest street */
        private double bestBikeSafety = 1;

        // track OSM nodes which are decomposed into multiple graph vertices because they are
        // elevators. later they will be iterated over to build ElevatorEdges between them.
        private HashMap<Long, HashMap<OSMLevel, IntersectionVertex>> multiLevelNodes =
                new HashMap<Long, HashMap<OSMLevel, IntersectionVertex>>();

        // track OSM nodes that will become graph vertices because they appear in multiple OSM ways
        private Map<Long, IntersectionVertex> intersectionNodes =
                new HashMap<Long, IntersectionVertex>();

        // track vertices to be removed in the turn-graph conversion.
        // this is a superset of intersectionNodes.values, which contains
        // a null vertex reference for multilevel nodes. the individual vertices
        // for each level of a multilevel node are includeed in endpoints.
        private ArrayList<IntersectionVertex> endpoints = new ArrayList<IntersectionVertex>();

        // track which vertical level each OSM way belongs to, for building elevators etc.
        private Map<OSMWay, OSMLevel> wayLevels = new HashMap<OSMWay, OSMLevel>();

        public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
            this.graph = graph;

            // handle turn restrictions, road names, and level maps in relations
            processRelations();

            // Remove all simple islands
            _nodes.keySet().retainAll(_nodesWithNeighbors);

            long wayIndex = 0;

            // figure out which nodes that are actually intersections
            initIntersectionNodes();

            GeometryFactory geometryFactory = new GeometryFactory();

            /* build an ordinary graph, which we will convert to an edge-based graph */

            for (OSMWay way : _ways.values()) {

                if (wayIndex % 10000 == 0)
                    _log.debug("ways=" + wayIndex + "/" + _ways.size());
                wayIndex++;

                WayProperties wayData = wayPropertySet.getDataForWay(way);

                if (!way.hasTag("name")) {
                    String creativeName = wayPropertySet.getCreativeNameForWay(way);
                    if (creativeName != null) {
                        way.addTag("otp:gen_name", creativeName);
                    }
                }
                Set<Alert> note = wayPropertySet.getNoteForWay(way);
                Set<Alert> wheelchairNote = getWheelchairNotes(way);

                StreetTraversalPermission permissions = getPermissionsForEntity(way,
                        wayData.getPermission());
                if (permissions == StreetTraversalPermission.NONE)
                    continue;

                List<Long> nodes = way.getNodeRefs();

                IntersectionVertex startEndpoint = null, endEndpoint = null;

                ArrayList<Coordinate> segmentCoordinates = new ArrayList<Coordinate>();

                getLevelsForWay(way);

                /*
                 * Traverse through all the nodes of this edge. For nodes which are not shared with
                 * any other edge, do not create endpoints -- just accumulate them for geometry and
                 * ele tags. For nodes which are shared, create endpoints and StreetVertex
                 * instances.
                 */
                Long startNode = null;
                //where the current edge should start
                OSMNode osmStartNode = null;
                List<ElevationPoint> elevationPoints = new ArrayList<ElevationPoint>();
                double distance = 0;
                for (int i = 0; i < nodes.size() - 1; i++) {
                    OSMNode segmentStartOSMNode = _nodes.get(nodes.get(i));
                    if (segmentStartOSMNode == null) {
                        continue;
                    }
                    Long endNode = nodes.get(i + 1);
                    if (osmStartNode == null) {
                        startNode = nodes.get(i);
                        osmStartNode = segmentStartOSMNode;
                        elevationPoints.clear();
                    }
                    //where the current edge might end
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
                    String ele = segmentStartOSMNode.getTag("ele");
                    if (ele != null) {
                        ele = ele.toLowerCase();
                        double unit = 1;
                        if (ele.endsWith("m")) {
                            ele = ele.replaceFirst("\\s*m", "");
                        } else if (ele.endsWith("ft")) {
                            ele = ele.replaceFirst("\\s*ft", "");
                            unit = 0.3048;
                        }
                        elevationPoints.add(new ElevationPoint(distance, Double.parseDouble(ele) * unit));
                    }

                    distance += DistanceLibrary.distance(segmentStartOSMNode.getLat(), segmentStartOSMNode.getLon(),
                            osmEndNode.getLat(), osmEndNode.getLon());

                    if (intersectionNodes.containsKey(endNode) || i == nodes.size() - 2) {
                        segmentCoordinates.add(getCoordinate(osmEndNode));
                        ele = osmEndNode.getTag("ele");
                        if (ele != null) {
                            elevationPoints.add(new ElevationPoint(distance, Double.parseDouble(ele)));
                        }

                        geometry = geometryFactory.createLineString(segmentCoordinates
                                .toArray(new Coordinate[0]));
                        segmentCoordinates.clear();
                    } else {
                        segmentCoordinates.add(getCoordinate(osmEndNode));
                        continue;
                    }

                    /* generate endpoints */
                    if (startEndpoint == null) { // first iteration on this way
                        // make or get a shared vertex for flat intersections,
                        // one vertex per level for multilevel nodes like elevators
                        startEndpoint = getVertexForOsmNode(osmStartNode, way);
                    } else { // subsequent iterations
                        startEndpoint = endEndpoint;
                    }

                    endEndpoint = getVertexForOsmNode(osmEndNode, way);

                    P2<PlainStreetEdge> streets = getEdgesForStreet(startEndpoint, endEndpoint,
                            way, i, permissions, geometry);

                    PlainStreetEdge street = streets.getFirst();

                    if (street != null) {
                        double safety = wayData.getSafetyFeatures().getFirst();
                        street.setBicycleSafetyEffectiveLength(street.getLength() * safety);
                        if (safety < bestBikeSafety) {
                            bestBikeSafety = safety;
                        }
                        if (note != null) {
                            street.setNote(note);
                        }
                        if (wheelchairNote != null) {
                            street.setWheelchairNote(wheelchairNote);
                        }
                    }

                    PlainStreetEdge backStreet = streets.getSecond();
                    if (backStreet != null) {
                        double safety = wayData.getSafetyFeatures().getSecond();
                        if (safety < bestBikeSafety) {
                            bestBikeSafety = safety;
                        }
                        backStreet.setBicycleSafetyEffectiveLength(backStreet.getLength() * safety);
                        if (note != null) {
                            backStreet.setNote(note);
                        }
                        if (wheelchairNote != null) {
                            backStreet.setWheelchairNote(wheelchairNote);
                        }
                    }

                    storeExtraElevationData(elevationPoints, street, backStreet, distance);

                    applyEdgesToTurnRestrictions(way, startNode, endNode, street, backStreet);
                    startNode = endNode;
                    osmStartNode = _nodes.get(startNode);
                }
            } // END loop over OSM ways

            buildElevatorEdges(graph);

            /* unify turn restrictions */
            Map<Edge, TurnRestriction> turnRestrictions = new HashMap<Edge, TurnRestriction>();
            for (TurnRestriction restriction : turnRestrictionsByTag.values()) {
                turnRestrictions.put(restriction.from, restriction);
            }
            if (customNamer != null) {
                customNamer.postprocess(graph);
            }
            
            //generate elevation profiles
            Map<EdgeWithElevation, List<ElevationPoint>> data = extraElevationData.data;
            for (Map.Entry<EdgeWithElevation, List<ElevationPoint>> entry : data.entrySet()) {
                EdgeWithElevation edge = entry.getKey();
                List<ElevationPoint> points = entry.getValue();
                Collections.sort(points);

                if (points.size() == 1) {
                    ElevationPoint firstPoint = points.get(0);
                    ElevationPoint endPoint = new ElevationPoint(edge.getDistance(), firstPoint.ele);
                    points.add(endPoint);
                }
                Coordinate[] coords = new Coordinate[points.size()];
                int i = 0;
                for (ElevationPoint p : points) {
                    double d = p.distanceAlongShape;
                    if (i == 0) {
                        d = 0;
                    } else if (i == points.size() - 1) {
                        d = edge.getDistance();
                    }
                    coords[i++] = new Coordinate(d, p.ele);
                }
                edge.setElevationProfile(new PackedCoordinateSequence.Double(coords), true);
            }

            applyBikeSafetyFactor(graph);
            StreetUtils.makeEdgeBased(graph, endpoints, turnRestrictions);

        } // END buildGraph()

        private void storeExtraElevationData(List<ElevationPoint> elevationPoints, PlainStreetEdge street, PlainStreetEdge backStreet, double length) {
            if (elevationPoints.isEmpty()) {
                return;
            }

            for (ElevationPoint p : elevationPoints) {
                MapUtils.addToMapList(extraElevationData.data, street, p);
                MapUtils.addToMapList(extraElevationData.data, backStreet, p.fromBack(length));
            }
        }

        private void buildElevatorEdges(Graph graph) {
            /* build elevator edges */
            for (Long nodeId : multiLevelNodes.keySet()) {
                OSMNode node = _nodes.get(nodeId);
                // this allows skipping levels, e.g., an elevator that stops
                // at floor 0, 2, 3, and 5.
                // Converting to an Array allows us to
                // subscript it so we can loop over it in twos. Assumedly, it will stay
                // sorted when we convert it to an Array.
                // The objects are Integers, but toArray returns Object[]
                HashMap<OSMLevel, IntersectionVertex> vertices = multiLevelNodes.get(nodeId);

                /*
                 * first, build FreeEdges to disconnect from the graph, GenericVertices to serve as
                 * attachment points, and ElevatorBoard and ElevatorAlight edges to connect future
                 * ElevatorHop edges to. After this iteration, graph will look like (side view):
                 * +==+~~X
                 * 
                 * +==+~~X
                 * 
                 * +==+~~X
                 * 
                 * + GenericVertex, X EndpointVertex, ~~ FreeEdge, ==
                 * ElevatorBoardEdge/ElevatorAlightEdge Another loop will fill in the
                 * ElevatorHopEdges.
                 */

                OSMLevel[] levels = vertices.keySet().toArray(new OSMLevel[0]);
                Arrays.sort(levels);
                ArrayList<Vertex> onboardVertices = new ArrayList<Vertex>();
                for (OSMLevel level : levels) {
                    // get the node to build the elevator out from
                    IntersectionVertex sourceVertex = vertices.get(level);
                    String sourceVertexLabel = sourceVertex.getLabel();
                    String levelName = level.longName;

                    ElevatorOffboardVertex offboardVertex = new ElevatorOffboardVertex(graph,
                            sourceVertexLabel + "_offboard", sourceVertex.getX(),
                            sourceVertex.getY(), levelName);

                    new FreeEdge(sourceVertex, offboardVertex);
                    new FreeEdge(offboardVertex, sourceVertex);

                    ElevatorOnboardVertex onboardVertex = new ElevatorOnboardVertex(graph,
                            sourceVertexLabel + "_onboard", sourceVertex.getX(),
                            sourceVertex.getY(), levelName);

                    new ElevatorBoardEdge(offboardVertex, onboardVertex);
                    new ElevatorAlightEdge(onboardVertex, offboardVertex, level.longName);

                    // accumulate onboard vertices to so they can be connected by hop edges later
                    onboardVertices.add(onboardVertex);
                }

                // -1 because we loop over onboardVertices two at a time
                for (Integer i = 0, vSize = onboardVertices.size() - 1; i < vSize; i++) {
                    Vertex from = onboardVertices.get(i);
                    Vertex to = onboardVertices.get(i + 1);

                    // default permissions: pedestrian, wheelchair, and bicycle
                    boolean wheelchairAccessible = true;
                    StreetTraversalPermission permission = StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;
                    // check for bicycle=no, otherwise assume it's OK to take a bike
                    if (node.isTagFalse("bicycle")) {
                        permission = StreetTraversalPermission.PEDESTRIAN;
                    }
                    // check for wheelchair=no
                    if (node.isTagFalse("wheelchair")) {
                        wheelchairAccessible = false;
                    }

                    // The narrative won't be strictly correct, as it will show the elevator as part
                    // of the cycling leg, but I think most cyclists will figure out that they
                    // should really dismount.
                    ElevatorHopEdge foreEdge = new ElevatorHopEdge(from, to, permission);
                    ElevatorHopEdge backEdge = new ElevatorHopEdge(to, from, permission);
                    foreEdge.wheelchairAccessible = wheelchairAccessible;
                    backEdge.wheelchairAccessible = wheelchairAccessible;
                }
            } // END elevator edge loop
        }

        private void applyEdgesToTurnRestrictions(OSMWay way, long startNode, long endNode,
                PlainStreetEdge street, PlainStreetEdge backStreet) {
            /* Check if there are turn restrictions starting on this segment */
            List<TurnRestrictionTag> restrictionTags = turnRestrictionsByFromWay.get(way.getId());

            if (restrictionTags != null) {
                for (TurnRestrictionTag tag : restrictionTags) {
                    if (tag.via == startNode) {
                        TurnRestriction restriction = turnRestrictionsByTag.get(tag);
                        restriction.from = backStreet;
                    } else if (tag.via == endNode) {
                        TurnRestriction restriction = turnRestrictionsByTag.get(tag);
                        restriction.from = street;
                    }
                }
            }

            restrictionTags = turnRestrictionsByToWay.get(way.getId());
            if (restrictionTags != null) {
                for (TurnRestrictionTag tag : restrictionTags) {
                    if (tag.via == startNode) {
                        TurnRestriction restriction = turnRestrictionsByTag.get(tag);
                        restriction.to = street;
                    } else if (tag.via == endNode) {
                        TurnRestriction restriction = turnRestrictionsByTag.get(tag);
                        restriction.to = backStreet;
                    }
                }
            }
        }

        private void getLevelsForWay(OSMWay way) {
            /* Determine OSM level for each way, if it was not already set */
            if (!wayLevels.containsKey(way)) {
                // if this way is not a key in the wayLevels map, a level map was not
                // already applied in processRelations

                /* try to find a level name in tags */
                String levelName = null;
                OSMLevel.Source source = OSMLevel.Source.NONE;
                OSMLevel level = OSMLevel.DEFAULT;
                if (way.hasTag("level")) { // TODO: floating-point levels &c.
                    levelName = way.getTag("level");
                    source = OSMLevel.Source.LEVEL_TAG;
                } else if (way.hasTag("layer")) {
                    levelName = way.getTag("layer");
                    source = OSMLevel.Source.LAYER_TAG;
                } 
                if (levelName != null) {
                    level = OSMLevel.fromString(levelName, source, noZeroLevels);
                }
                wayLevels.put(way, level);
            }
        }

        private void initIntersectionNodes() {
            Set<Long> possibleIntersectionNodes = new HashSet<Long>();
            for (OSMWay way : _ways.values()) {
                List<Long> nodes = way.getNodeRefs();
                for (long node : nodes) {
                    if (possibleIntersectionNodes.contains(node)) {
                        intersectionNodes.put(node, null);
                    } else {
                        possibleIntersectionNodes.add(node);
                    }
                }
            }
        }

        private Set<Alert> getWheelchairNotes(OSMWithTags way) {
            Map<String, String> tags = way.getTagsByPrefix("wheelchair:description");
            if (tags == null) {
                return null;
            }
            Set<Alert> alerts = new HashSet<Alert>();
            Alert alert = new Alert();
            alerts.add(alert);
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                if (k.equals("wheelchair:description")) {
                    // no language, assume default from TranslatedString
                    alert.alertHeaderText = new TranslatedString(v);
                } else {
                    String lang = k.substring("wheelchair:description:".length());
                    alert.alertHeaderText = new TranslatedString(lang, v);
                }
            }
            return alerts;
        }

        /**
         * The safest bike lane should have a safety weight no lower than the time weight of a flat
         * street. This method divides the safety lengths by the length ratio of the safest street,
         * ensuring this property.
         * 
         * @param graph
         */
        private void applyBikeSafetyFactor(Graph graph) {
            _log.info(GraphBuilderAnnotation.register(graph, Variety.GRAPHWIDE,
                    "Multiplying all bike safety values by " + (1 / bestBikeSafety)));
            HashSet<Edge> seenEdges = new HashSet<Edge>();
            for (Vertex vertex : graph.getVertices()) {
                for (Edge e : vertex.getOutgoing()) {
                    if (!(e instanceof PlainStreetEdge)) {
                        continue;
                    }
                    PlainStreetEdge pse = (PlainStreetEdge) e;

                    if (!seenEdges.contains(e)) {
                        seenEdges.add(e);
                        pse.setBicycleSafetyEffectiveLength(pse.getBicycleSafetyEffectiveLength()
                                / bestBikeSafety);
                    }
                }
                for (Edge e : vertex.getIncoming()) {
                    if (!(e instanceof PlainStreetEdge)) {
                        continue;
                    }
                    PlainStreetEdge pse = (PlainStreetEdge) e;

                    if (!seenEdges.contains(e)) {
                        seenEdges.add(e);
                        pse.setBicycleSafetyEffectiveLength(pse.getBicycleSafetyEffectiveLength()
                                / bestBikeSafety);
                    }
                }
            }
        }

        private Coordinate getCoordinate(OSMNode osmNode) {
            return new Coordinate(osmNode.getLon(), osmNode.getLat());
        }

        public void addNode(OSMNode node) {
            if (!_nodesWithNeighbors.contains(node.getId()))
                return;

            if (_nodes.containsKey(node.getId()))
                return;

            _nodes.put(node.getId(), node);

            if (_nodes.size() % 100000 == 0)
                _log.debug("nodes=" + _nodes.size());
        }

        public void addWay(OSMWay way) {
            if (_ways.containsKey(way.getId()))
                return;

            _ways.put(way.getId(), way);

            if (_ways.size() % 10000 == 0)
                _log.debug("ways=" + _ways.size());
        }

        public void addRelation(OSMRelation relation) {
            if (_relations.containsKey(relation.getId()))
                return;

            if (!(relation.isTag("type", "restriction"))
                    && !(relation.isTag("type", "route") && relation.isTag("route", "road"))
                    && !(relation.isTag("type", "multipolygon") && relation.hasTag("highway"))
                    && !(relation.isTag("type", "level_map"))) {
                return;
            }

            _relations.put(relation.getId(), relation);

            if (_relations.size() % 100 == 0)
                _log.debug("relations=" + _relations.size());

        }

        public void secondPhase() {
            int count = _ways.values().size();

            // This copies relevant tags to the ways (highway=*) where it doesn't exist, so that
            // the way purging keeps the needed way around.
            // Multipolygons may be processed more than once, which may be needed since
            // some member might be in different files for the same multipolygon.
            processMultipolygons();

            for (Iterator<OSMWay> it = _ways.values().iterator(); it.hasNext();) {
                OSMWay way = it.next();
                if (!(way.hasTag("highway") || way.isTag("railway", "platform"))) {
                    it.remove();
                } else if (way.isTag("highway", "conveyer") || way.isTag("highway", "proposed")) {
                    it.remove();
                } else if (way.isTag("area", "yes")) {
                    // routing on areas is not yet supported. areas can cause problems with stop linking.
                    // (24th & Mission BART plaza is both highway=pedestrian and area=yes)
                    it.remove(); 
                } else {
                    // Since the way is kept, update nodes-with-neighbors
                    List<Long> nodes = way.getNodeRefs();
                    if (nodes.size() > 1) {
                        _nodesWithNeighbors.addAll(nodes);
                    }
                }
            }

            _log.trace("purged " + (count - _ways.values().size()) + " ways out of " + count);
        }

        /**
         * Copies useful metadata from multipolygon relations to the relevant ways.
         * 
         * This is done at a different time than processRelations(), so that way purging doesn't
         * remove the used ways.
         */
        private void processMultipolygons() {
            for (OSMRelation relation : _relations.values()) {
                if (!(relation.isTag("type", "multipolygon") && relation.hasTag("highway"))) {
                    continue;
                }

                for (OSMRelationMember member : relation.getMembers()) {
                    if (!("way".equals(member.getType()) && _ways.containsKey(member.getRef()))) {
                        continue;
                    }

                    OSMWithTags way = _ways.get(member.getRef());
                    if (way == null) {
                        continue;
                    }

                    if (relation.hasTag("highway") && !way.hasTag("highway")) {
                        way.addTag("highway", relation.getTag("highway"));
                    }
                    if (relation.hasTag("name") && !way.hasTag("name")) {
                        way.addTag("name", relation.getTag("name"));
                    }
                    if (relation.hasTag("ref") && !way.hasTag("ref")) {
                        way.addTag("ref", relation.getTag("ref"));
                    }
                }
            }
        }

        /**
         * Copies useful metadata from relations to the relevant ways/nodes.
         */
        private void processRelations() {
            _log.debug("Processing relations...");

            for (OSMRelation relation : _relations.values()) {
                if (relation.isTag("type", "restriction")) {
                    processRestriction(relation);
                } else if (relation.isTag("type", "level_map")) {
                    processLevelMap(relation);
                } else if (relation.isTag("type", "route")) {
                    processRoad(relation);
                }
                // multipolygons were already processed in secondPhase()
            }
        }

        /**
         * A temporary holder for turn restrictions while we have only way/node ids but not yet edge
         * objects
         */
        class TurnRestrictionTag {
            private long via;

            private TurnRestrictionType type;

            TurnRestrictionTag(long via, TurnRestrictionType type) {
                this.via = via;
                this.type = type;
            }
        }

        /**
         * Handle turn restrictions
         * 
         * @param relation
         */
        private void processRestriction(OSMRelation relation) {
            long from = -1, to = -1, via = -1;
            for (OSMRelationMember member : relation.getMembers()) {
                String role = member.getRole();
                if (role.equals("from")) {
                    from = member.getRef();
                } else if (role.equals("to")) {
                    to = member.getRef();
                } else if (role.equals("via")) {
                    via = member.getRef();
                }
            }
            if (from == -1 || to == -1 || via == -1) {
                _log.warn(GraphBuilderAnnotation.register(graph, Variety.TURN_RESTRICTION_BAD,
                        relation.getId()));
                return;
            }

            Set<TraverseMode> modes = EnumSet.of(TraverseMode.BICYCLE, TraverseMode.CAR);
            String exceptModes = relation.getTag("except");
            if (exceptModes != null) {
                for (String m : exceptModes.split(";")) {
                    if (m.equals("motorcar")) {
                        modes.remove(TraverseMode.CAR);
                    } else if (m.equals("bicycle")) {
                        modes.remove(TraverseMode.BICYCLE);
                        _log.warn(GraphBuilderAnnotation.register(graph,
                                Variety.TURN_RESTRICTION_EXCEPTION, via, from));
                    }
                }
            }
            modes = TraverseMode.internSet(modes);

            TurnRestrictionTag tag;
            if (relation.isTag("restriction", "no_right_turn")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN);
            } else if (relation.isTag("restriction", "no_left_turn")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN);
            } else if (relation.isTag("restriction", "no_straight_on")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN);
            } else if (relation.isTag("restriction", "no_u_turn")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN);
            } else if (relation.isTag("restriction", "only_straight_on")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN);
            } else if (relation.isTag("restriction", "only_right_turn")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN);
            } else if (relation.isTag("restriction", "only_left_turn")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN);
            } else {
                _log.warn(GraphBuilderAnnotation.register(graph, Variety.TURN_RESTRICTION_UNKNOWN,
                        relation.getTag("restriction")));
                return;
            }
            TurnRestriction restriction = new TurnRestriction();
            restriction.type = tag.type;
            restriction.modes = modes;
            turnRestrictionsByTag.put(tag, restriction);

            MapUtils.addToMapList(turnRestrictionsByFromWay, from, tag);
            MapUtils.addToMapList(turnRestrictionsByToWay, to, tag);

        }

        /**
         * Process an OSM level map.
         * 
         * @param relation
         */
        private void processLevelMap(OSMRelation relation) {
            Map<String, OSMLevel> levels = OSMLevel.mapFromSpecList(relation.getTag("levels"), Source.LEVEL_MAP, true);
            for (OSMRelationMember member : relation.getMembers()) {
                if ("way".equals(member.getType()) && _ways.containsKey(member.getRef())) {
                    OSMWay way = _ways.get(member.getRef());
                    if (way != null) {
                        String role = member.getRole();
                        // if the level map relation has a role:xyz tag, this way is something
                        // more complicated than a single level (e.g. ramp/stairway).
                        if (!relation.hasTag("role:" + role)) {
                            if (levels.containsKey(role)) {
                                wayLevels.put(way, levels.get(role));
                            } else {
                                _log.warn(member.getRef() + " has undefined level " + role);
                            }
                        }
                    }
                }
            }
        }

        /*
         * Handle route=road relations.
         * 
         * @param relation
         */
        private void processRoad(OSMRelation relation) {
            for (OSMRelationMember member : relation.getMembers()) {
                if (!("way".equals(member.getType()) && _ways.containsKey(member.getRef()))) {
                    continue;
                }

                OSMWithTags way = _ways.get(member.getRef());
                if (way == null) {
                    continue;
                }

                if (relation.hasTag("name")) {
                    if (way.hasTag("otp:route_name")) {
                        way.addTag(
                                "otp:route_name",
                                addUniqueName(way.getTag("otp:route_name"), relation.getTag("name")));
                    } else {
                        way.addTag(new OSMTag("otp:route_name", relation.getTag("name")));
                    }
                }
                if (relation.hasTag("ref")) {
                    if (way.hasTag("otp:route_ref")) {
                        way.addTag("otp:route_ref",
                                addUniqueName(way.getTag("otp:route_ref"), relation.getTag("ref")));
                    } else {
                        way.addTag(new OSMTag("otp:route_ref", relation.getTag("ref")));
                    }
                }
            }
        }

        private String addUniqueName(String routes, String name) {
            String[] names = routes.split(", ");
            for (String existing : names) {
                if (existing.equals(name)) {
                    return routes;
                }
            }
            return routes + ", " + name;
        }

        /**
         * Handle oneway streets, cycleways, and whatnot. See
         * http://wiki.openstreetmap.org/wiki/Bicycle for various scenarios, along with
         * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Oneway.
         * 
         * @param end
         * @param start
         */
        private P2<PlainStreetEdge> getEdgesForStreet(IntersectionVertex start,
                IntersectionVertex end, OSMWithTags way, long startNode,
                StreetTraversalPermission permissions, LineString geometry) {
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
             * pedestrian rules: everything is two-way (assuming pedestrians are allowed at all)
             * bicycle rules: default: permissions;
             * 
             * cycleway=dismount means walk your bike -- the engine will automatically try walking
             * bikes any time it is forbidden to ride them, so the only thing to do here is to
             * remove bike permissions
             * 
             * oneway=... sets permissions for cars and bikes oneway:bicycle overwrites these
             * permissions for bikes only
             * 
             * now, cycleway=opposite_lane, opposite, opposite_track can allow once oneway has been
             * set by oneway:bicycle, but should give a warning if it conflicts with oneway:bicycle
             * 
             * bicycle:backward=yes works like oneway:bicycle=no bicycle:backwards=no works like
             * oneway:bicycle=yes
             */

            String foot = way.getTag("foot");
            if ("yes".equals(foot) || "designated".equals(foot)) {
                permissions = permissions.add(StreetTraversalPermission.PEDESTRIAN);
            }

            if (OSMWithTags.isFalse(foot)) {
                permissions = permissions.remove(StreetTraversalPermission.PEDESTRIAN);
            }

            boolean forceBikes = false;
            String bicycle = way.getTag("bicycle");
            if ("yes".equals(bicycle) || "designated".equals(bicycle)) {
                permissions = permissions.add(StreetTraversalPermission.BICYCLE);
                forceBikes = true;
            }

            if (way.isTag("cycleway", "dismount") || "dismount".equals(bicycle)) {
                permissions = permissions.remove(StreetTraversalPermission.BICYCLE);
                if (forceBikes) {
                    _log.warn(GraphBuilderAnnotation.register(graph, Variety.CONFLICTING_BIKE_TAGS,
                            way.getId()));
                }
            }

            StreetTraversalPermission permissionsFront = permissions;
            StreetTraversalPermission permissionsBack = permissions;

            if (way.isTagTrue("oneway") || "roundabout".equals(tags.get("junction"))) {
                permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE_AND_CAR);
            }
            if (way.isTag("oneway", "-1")) {
                permissionsFront = permissionsFront
                        .remove(StreetTraversalPermission.BICYCLE_AND_CAR);
            }
            String oneWayBicycle = way.getTag("oneway:bicycle");
            if (OSMWithTags.isTrue(oneWayBicycle) || way.isTagFalse("bicycle:backwards")) {
                permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE);
            }
            if ("-1".equals(oneWayBicycle)) {
                permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE);
            }
            if (OSMWithTags.isFalse(oneWayBicycle) || way.isTagTrue("bicycle:backwards")) {
                if (permissions.allows(StreetTraversalPermission.BICYCLE)) {
                    permissionsFront = permissionsFront.add(StreetTraversalPermission.BICYCLE);
                    permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
                }
            }

            // any cycleway which is opposite* allows contraflow biking
            String cycleway = way.getTag("cycleway");
            String cyclewayLeft = way.getTag("cycleway:left");
            String cyclewayRight = way.getTag("cycleway:right");
            if ((cycleway != null && cycleway.startsWith("opposite"))
                    || (cyclewayLeft != null && cyclewayLeft.startsWith("opposite"))
                    || (cyclewayRight != null && cyclewayRight.startsWith("opposite"))) {

                permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
            }

            String access = way.getTag("access");
            boolean noThruTraffic = "destination".equals(access) || "private".equals(access)
                    || "customers".equals(access) || "delivery".equals(access)
                    || "forestry".equals(access) || "agricultural".equals(access);

            if (permissionsFront != StreetTraversalPermission.NONE) {
                street = getEdgeForStreet(start, end, way, startNode, d, permissionsFront,
                        geometry, false);
                street.setNoThruTraffic(noThruTraffic);
            }
            if (permissionsBack != StreetTraversalPermission.NONE) {
                backStreet = getEdgeForStreet(end, start, way, startNode, d, permissionsBack,
                        backGeometry, true);
                backStreet.setNoThruTraffic(noThruTraffic);
            }

            /* mark edges that are on roundabouts */
            if ("roundabout".equals(tags.get("junction"))) {
                if (street != null)
                    street.setRoundabout(true);
                if (backStreet != null)
                    backStreet.setRoundabout(true);
            }

            return new P2<PlainStreetEdge>(street, backStreet);
        }

        private PlainStreetEdge getEdgeForStreet(IntersectionVertex start, IntersectionVertex end,
                OSMWithTags way, long startNode, double length,
                StreetTraversalPermission permissions, LineString geometry, boolean back) {

            String id = "way " + way.getId() + " from " + startNode;
            id = unique(id);

            String name = way.getAssumedName();

            if (customNamer != null) {
                name = customNamer.name(way, name);
            }

            if (name == null) {
                name = id;
            }

            boolean steps = "steps".equals(way.getTag("highway"));
            if (steps) {
                // consider the elevation gain of stairs, roughly
                length *= 2;
            }

            PlainStreetEdge street = new PlainStreetEdge(start, end, geometry, name, length,
                    permissions, back);
            street.setId(id);

            if (!way.hasTag("name")) {
                street.setBogusName(true);
            }
            street.setStairs(steps);

            /* TODO: This should probably generalized somehow? */
            if (way.isTagFalse("wheelchair") || (steps && !way.isTagTrue("wheelchair"))) {
                street.setWheelchairAccessible(false);
            }

            street.setSlopeOverride(wayPropertySet.getSlopeOverride(way));

            if (customNamer != null) {
                customNamer.nameWithEdge(way, street);
            }

            return street;
        }

        private StreetTraversalPermission getPermissionsForEntity(OSMWithTags entity,
                StreetTraversalPermission def) {
            Map<String, String> tags = entity.getTags();
            StreetTraversalPermission permission = null;

            String highway = tags.get("highway");
            String cycleway = tags.get("cycleway");
            String access = tags.get("access");
            String motorcar = tags.get("motorcar");
            String bicycle = tags.get("bicycle");
            String foot = tags.get("foot");

            /*
             * Only a few tags are examined here, because we only care about modes supported by OTP
             * (wheelchairs are not of concern here)
             * 
             * Only a few values are checked for, all other values are presumed to be permissive (=>
             * This may not be perfect, but is closer to reality, since most people don't follow the
             * rules perfectly ;-)
             */
            if (access != null) {
                if ("no".equals(access) || "license".equals(access)) {
                    // this can actually be overridden
                    permission = StreetTraversalPermission.NONE;
                    if (entity.doesTagAllowAccess("motorcar")) {
                        permission = permission.add(StreetTraversalPermission.CAR);
                    }
                    if (entity.doesTagAllowAccess("bicycle")) {
                        permission = permission.add(StreetTraversalPermission.BICYCLE);
                    }
                    if (entity.doesTagAllowAccess("foot")) {
                        permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
                    }
                } else {
                    permission = def;
                }
            } else if (motorcar != null || bicycle != null || foot != null) {
                permission = def;
            }

            if (motorcar != null) {
                if ("no".equals(motorcar) || "license".equals(motorcar)) {
                    permission = permission.remove(StreetTraversalPermission.CAR);
                } else {
                    permission = permission.add(StreetTraversalPermission.CAR);
                }
            }

            if (bicycle != null) {
                if ("no".equals(bicycle) || "license".equals(bicycle)) {
                    permission = permission.remove(StreetTraversalPermission.BICYCLE);
                } else {
                    permission = permission.add(StreetTraversalPermission.BICYCLE);
                }
            }

            if (foot != null) {
                if ("no".equals(foot) || "license".equals(foot)) {
                    permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
                } else {
                    permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
                }
            }

            if (highway != null) {
                if ("construction".equals(highway)) {
                    permission = StreetTraversalPermission.NONE;
                }
            } else {
                if ("construction".equals(cycleway)) {
                    permission = StreetTraversalPermission.NONE;
                }
            }

            if (permission == null)
                return def;

            return permission;
        }

        /**
         * Is this a multi-level node that should be decomposed to multiple coincident nodes?
         * Currently returns true only for elevators.
         * 
         * @param node
         * @return whether the node is multi-level
         * @author mattwigway
         */
        private boolean isMultiLevelNode(OSMNode node) {
            return node.hasTag("highway") && "elevator".equals(node.getTag("highway"));
        }

        /**
         * Record the level of the way for this node, e.g. if the way is at level 5, mark that this
         * node is active at level 5.
         * 
         * @param the way that has the level
         * @param the node to record for
         * @author mattwigway
         */
        private IntersectionVertex recordLevel(OSMNode node, OSMWithTags way) {
            OSMLevel level = wayLevels.get(way);
            HashMap<OSMLevel, IntersectionVertex> vertices;
            long nodeId = node.getId();
            if (multiLevelNodes.containsKey(nodeId)) {
                vertices = multiLevelNodes.get(nodeId);
            } else {
                vertices = new HashMap<OSMLevel, IntersectionVertex>();
                multiLevelNodes.put(nodeId, vertices);
            }
            if (!vertices.containsKey(level)) {
                Coordinate coordinate = getCoordinate(node);
                String label = "osm node " + nodeId + " at level " + level.shortName;
                IntersectionVertex vertex = new IntersectionVertex(graph, label, coordinate.x,
                        coordinate.y, label);
                vertices.put(level, vertex);
                // multilevel nodes should also undergo turn-conversion
                endpoints.add(vertex);
                return vertex;
            }
            return vertices.get(level);
        }

        /**
         * Make or get a shared vertex for flat intersections, or one vertex per level for
         * multilevel nodes like elevators. When there is an elevator or other Z-dimension
         * discontinuity, a single node can appear in several ways at different levels.
         * 
         * @param node The node to fetch a label for.
         * @param way The way it is connected to (for fetching level information).
         * @return vertex The graph vertex.
         */
        private IntersectionVertex getVertexForOsmNode(OSMNode node, OSMWithTags way) {
            // If the node should be decomposed to multiple levels,
            // use the numeric level because it is unique, the human level may not be (although
            // it will likely lead to some head-scratching if it is not).
            IntersectionVertex iv = null;
            if (isMultiLevelNode(node)) {
                // make a separate node for every level
                return recordLevel(node, way);
            }
            // single-level case
            long nid = node.getId();
            iv = intersectionNodes.get(nid);
            if (iv == null) {
                Coordinate coordinate = getCoordinate(node);
                String label = "osm node " + nid;
                iv = new IntersectionVertex(graph, label, coordinate.x, coordinate.y, label);
                intersectionNodes.put(nid, iv);
                endpoints.add(iv);
            }
            return iv;
        }
    }

    public CustomNamer getCustomNamer() {
        return customNamer;
    }

    public void setCustomNamer(CustomNamer customNamer) {
        this.customNamer = customNamer;
    }
}
