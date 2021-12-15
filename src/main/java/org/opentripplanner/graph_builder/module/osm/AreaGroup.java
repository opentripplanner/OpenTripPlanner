package org.opentripplanner.graph_builder.module.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.common.DisjointSet;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.module.osm.Ring.RingConstructionException;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

/**
 * A group of possibly-contiguous areas sharing the same level
 */
class AreaGroup {

    private static Logger LOG = LoggerFactory.getLogger(AreaGroup.class);

    /*
     * The list of underlying areas, used when generating edges out of the visibility graph
     */
    Collection<Area> areas;

    /**
     * The joined outermost rings of the areas (with inner rings for holes as necessary).
     */
    List<Ring> outermostRings = new ArrayList<Ring>();

    public AreaGroup(Collection<Area> areas) {
        this.areas = areas;

        // Merging non-convex polygons is complicated, so we need to convert to JTS, let JTS do the
        // hard work,
        // then convert back.
        List<Polygon> allRings = new ArrayList<Polygon>();

        // However, JTS will lose the coord<->osmnode mapping, and we will have to reconstruct it.
        HashMap<Coordinate, OSMNode> nodeMap = new HashMap<Coordinate, OSMNode>();
        for (Area area : areas) {
            for (Ring ring : area.outermostRings) {
                allRings.add(ring.jtsPolygon);
                for (OSMNode node : ring.nodes) {
                    nodeMap.put(new Coordinate(node.lon, node.lat), node);
                }
                for (Ring inner : ring.getHoles()) {
                    for (OSMNode node : inner.nodes) {
                        nodeMap.put(new Coordinate(node.lon, node.lat), node);
                    }
                }
            }
        }
        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
        Geometry u = geometryFactory.createMultiPolygon(allRings.toArray(new Polygon[allRings
                .size()]));
        u = u.union();

        if (u instanceof GeometryCollection) {
            GeometryCollection mp = (GeometryCollection) u;
            for (int i = 0; i < mp.getNumGeometries(); ++i) {
                Geometry poly = mp.getGeometryN(i);
                if (!(poly instanceof Polygon)) {
                    LOG.warn("Unexpected non-polygon when merging areas: " + poly);
                    continue;
                }
                outermostRings.add(toRing((Polygon) poly, nodeMap));
            }
        } else if (u instanceof Polygon) {
            outermostRings.add(toRing((Polygon) u, nodeMap));
        } else {
            LOG.warn("Unexpected non-polygon when merging areas: " + u);
        }
    }

    public static List<AreaGroup> groupAreas(Map<Area, OSMLevel> areasLevels) {
        DisjointSet<Area> groups = new DisjointSet<Area>();
        Multimap<OSMNode, Area> areasForNode = LinkedListMultimap.create();
        for (Area area : areasLevels.keySet()) {
            for (Ring ring : area.outermostRings) {
                for (Ring inner : ring.getHoles()) {
                    for (OSMNode node : inner.nodes) {
                        areasForNode.put(node, area);
                    }
                }
                for (OSMNode node : ring.nodes) {
                    areasForNode.put(node, area);
                }
            }
        }

        // areas that can be joined must share nodes and levels
        for (OSMNode osmNode : areasForNode.keySet()) {
            for (Area area1 : areasForNode.get(osmNode)) {
                OSMLevel level1 = areasLevels.get(area1);
                for (Area area2 : areasForNode.get(osmNode)) {
                    OSMLevel level2 = areasLevels.get(area2);
                    if ((level1 == null && level2 == null)
                            || (level1 != null && level1.equals(level2))) {
                        groups.union(area1, area2);
                    }
                }
            }
        }

        List<AreaGroup> out = new ArrayList<AreaGroup>();
        for (Set<Area> areaSet : groups.sets()) {
            try {
                out.add(new AreaGroup(areaSet));
            } catch (RingConstructionException e) {
                for (Area area : areaSet) {
                    LOG.debug("Failed to create merged area for "
                            + area
                            + ".  This area might not be at fault; it might be one of the other areas in this list.");
                    out.add(new AreaGroup(Arrays.asList(area)));
                }
            }
        }
        return out;
    }

    private Ring toRing(Polygon polygon, HashMap<Coordinate, OSMNode> nodeMap) {
        List<OSMNode> shell = new ArrayList<OSMNode>();
        for (Coordinate coord : polygon.getExteriorRing().getCoordinates()) {
            OSMNode node = nodeMap.get(coord);
            if (node == null) {
                throw new RingConstructionException();
            }
            shell.add(node);
        }
        Ring ring = new Ring(shell);
        // now the holes
        for (int i = 0; i < polygon.getNumInteriorRing(); ++i) {
            LineString interior = polygon.getInteriorRingN(i);
            List<OSMNode> hole = new ArrayList<OSMNode>();
            for (Coordinate coord : interior.getCoordinates()) {
                OSMNode node = nodeMap.get(coord);
                if (node == null) {
                    throw new RingConstructionException();
                }
                hole.add(node);
            }
            ring.addHole(new Ring(hole));
        }

        return ring;
    }

    public OSMWithTags getSomeOSMObject() {
        return areas.iterator().next().parent;
    }
}
