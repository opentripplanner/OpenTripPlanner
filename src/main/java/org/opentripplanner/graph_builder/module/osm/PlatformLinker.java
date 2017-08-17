package org.opentripplanner.graph_builder.module.osm;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.services.StreetEdgeFactory;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Links unconnected entries to platforms. The entries may be stairs or walk paths in OSM
 * arriving inside the platform area. The end vertex of the entry is linked to all vertices in
 * the ring defining the platform.
 *
 * An implementation of the Ray-casting algorithm is used to decide if the endpoint of the entry
 * is inside the platform area.
 *
 */
public class PlatformLinker {

    private static Logger LOG = LoggerFactory.getLogger(PlatformLinker.class);

    public static GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

    private Graph graph;
    private OSMDatabase osmdb;
    private StreetEdgeFactory factory;
    private CustomNamer customNamer;


    PlatformLinker(Graph graph, OSMDatabase osmdb, StreetEdgeFactory factory, CustomNamer customNamer) {
        this.graph = graph;
        this.osmdb = osmdb;
        this.factory = factory;
        this.customNamer = customNamer;
    }

    void linkEntriesToPlatforms() {

        LOG.info("Start linking platforms");
        List<OsmVertex> endpoints = graph.getVertices().stream().
                filter(OsmVertex.class::isInstance).
                map(OsmVertex.class::cast).
                filter(this::isEndpoint).
                collect(Collectors.toList());

        LOG.info("Endpoints found: " + endpoints.size());


        List<Area> platforms = osmdb.getWalkableAreas().stream().
                filter(area -> "platform".equals(area.parent.getTag("public_transport")) &&
                        "platform".equals(area.parent.getTag("railway"))).
                collect(Collectors.toList());

        LOG.info("Platforms found: " + platforms.size());

        for (Area area : platforms) {
            List<OsmVertex> endpointsWithin = new ArrayList<>();
            List<Ring> rings = area.outermostRings;
            for (Ring ring : rings) {
                endpointsWithin.addAll(endpoints.stream().filter(t -> contains(ring, t)).collect(Collectors.toList()));

                for (OSMNode node : ring.nodes) {
                    Vertex vertexById = graph.getVertex("osm:node:" + node.getId());
                    if (vertexById != null) {
                        endpointsWithin.forEach(e -> makePlatformEdges(area, e, (OsmVertex) vertexById));
                    }
                }

            }
        }
        LOG.info("Done linking platforms");
    }

    private boolean isEndpoint(OsmVertex ov) {
        boolean isCandidate = false;
        Vertex start = null;
        for (Edge e : ov.getIncoming()) {
            if (e instanceof StreetEdge && ! (e instanceof AreaEdge)) {
                StreetEdge se = (StreetEdge) e;
                if (Arrays.asList(1,2,3).contains(se.getPermission().code)) {
                    isCandidate = true;
                    start = se.getFromVertex();
                    break;
                }
            }
        }

        if (isCandidate && start != null) {
            boolean isEndpoint = true;
            for (Edge se : ov.getOutgoing()) {
                if (!se.getToVertex().getCoordinate().equals(start.getCoordinate()) && !(se instanceof AreaEdge)) {
                    isEndpoint = false;
                }
            }
            return isEndpoint;
        }
        return false;
    }

    private void makePlatformEdges(Area area, OsmVertex from, OsmVertex to) {
        Coordinate[] coordinates = new Coordinate[] { from.getCoordinate(),
                to.getCoordinate() };
        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
        LineString line = geometryFactory.createLineString(coordinates);

        double length = SphericalDistanceLibrary.distance(from.getCoordinate(),
                to.getCoordinate());

        StreetTraversalPermission areaPermissions = OSMFilter.getPermissionsForEntity(
                area.parent, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

        String labelFromTo = "way (area) " + area.parent.getId() + " from " + from.getLabel()
                + " to " + to.getLabel();
        I18NString nameFromTo = getNameForWay(area.parent, labelFromTo);
        factory.createEdge(from, to, line, nameFromTo, length, areaPermissions, true);

        String labelToFrom = "way (area) " + area.parent.getId() + " from " + to.getLabel()
                + " to " + from.getLabel();
        I18NString nameToFrom = getNameForWay(area.parent, labelToFrom);
        factory.createEdge(to, from, line, nameToFrom, length, areaPermissions, true);
    }

    private I18NString getNameForWay(OSMWithTags way, String id) {
        I18NString name = way.getAssumedName();

        if (customNamer != null && name != null) {
            name = new NonLocalizedString(customNamer.name(way, name.toString()));
        }

        if (name == null) {
            name = new NonLocalizedString(id);
        }
        return name;
    }

    /**
     * Returns true if the vertex is inside the area defined by this Ring object.
     */
    static boolean contains(Ring ring, Vertex vertex) {
        double[][] shape = new double[ring.nodes.size()][2];
        for (int i = 0; i < ring.nodes.size(); i++) {
            shape[i][0] = ring.nodes.get(i).lon;
            shape[i][1] = ring.nodes.get(i).lat;
        }

        double[] pnt = {vertex.getLon(), vertex.getLat()};
        return contains(shape, pnt);
    }

    static boolean contains(double[][] shape, double[] pnt) {
        boolean inside = false;
        int len = shape.length;
        for (int i = 0; i < len; i++) {
            if (intersects(shape[i], shape[(i + 1) % len], pnt))
                inside = !inside;
        }
        return inside;
    }

    private static boolean intersects(double[] a, double[] b, double[] P) {
        if (a[1] > b[1])
            return intersects(b, a, P);

        if (P[1] == a[1] || P[1] == b[1])
            P[1] += 0.000000000000010;

        if (P[1] > b[1] || P[1] < a[1] || P[0] > max(a[0], b[0]))
            return false;

        if (P[0] < min(a[0], b[0]))
            return true;

        double red = (P[1] - a[1]) / (P[0] - a[0]);
        double blue = (b[1] - a[1]) / (b[0] - a[0]);
        return red >= blue;
    }


}
