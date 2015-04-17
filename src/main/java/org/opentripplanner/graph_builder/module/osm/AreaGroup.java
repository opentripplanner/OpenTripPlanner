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

package org.opentripplanner.graph_builder.module.osm;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.*;
import org.opentripplanner.common.DisjointSet;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import com.conveyal.osmlib.Node;
import com.conveyal.osmlib.OSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    List<Ring> outermostRings = Lists.newArrayList();

    /** A group of areas that share at least one node and are on the same OSM level. */
    public AreaGroup(Collection<Area> areas, OSM osm) {

        this.areas = areas;

        // Merging non-convex polygons is complicated, so we need to convert to JTS, let JTS do the
        // hard work,
        // then convert back.
        List<Polygon> allRings = new ArrayList<Polygon>();

        // However, JTS will lose the coord<->osmnode mapping, and we will have to reconstruct it.
        Map<Coordinate, Long> nodeMap = Maps.newHashMap();
        for (Area area : areas) {
            for (Ring ring : area.outermostRings) {
                allRings.add(ring.toJtsPolygon());
                for (long nodeId : ring.nodeIds) {
                    Node node = osm.nodes.get(nodeId);
                    nodeMap.put(new Coordinate(node.getLon(), node.getLat()), nodeId);
                }
                for (Ring inner : ring.holes) {
                    for (long nodeId : inner.nodeIds) {
                        Node node = osm.nodes.get(nodeId);
                        nodeMap.put(new Coordinate(node.getLon(), node.getLat()), nodeId);
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
                outermostRings.add(toRing((Polygon) poly, nodeMap, osm));
            }
        } else if (u instanceof Polygon) {
            outermostRings.add(toRing((Polygon) u, nodeMap, osm));
        } else {
            LOG.warn("Unexpected non-polygon when merging areas: " + u);
        }
    }

    /**
     * Group areas together into AreaGroups if they share at least one node and are on the same level.
     * TODO call this with a pre-built Map<Area, OSMLevel> in OSMDB.
     */
    public static List<AreaGroup> groupAreas(Map<Area, OSMLevel> areasLevels, OSM osm) {
        DisjointSet<Area> groups = new DisjointSet<Area>();
        Multimap<Long, Area> areasForNode = LinkedListMultimap.create(); // Why linked list? Are we inserting?
        for (Area area : areasLevels.keySet()) {
            for (Ring ring : area.outermostRings) {
                for (Ring inner : ring.holes) {
                    for (long nodeId : inner.nodeIds) {
                        areasForNode.put(nodeId, area);
                    }
                }
                for (long nodeId : ring.nodeIds) {
                    areasForNode.put(nodeId, area);
                }
            }
        }

        // areas that can be joined must share nodes and levels
        for (long osmNodeId : areasForNode.keySet()) {
            for (Area area1 : areasForNode.get(osmNodeId)) {
                OSMLevel level1 = areasLevels.get(area1);
                for (Area area2 : areasForNode.get(osmNodeId)) {
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
                out.add(new AreaGroup(areaSet, osm));
            } catch (AreaGroup.RingConstructionException e) {
                for (Area area : areaSet) {
                    LOG.debug("Failed to create merged area for "
                            + area
                            + ".  This area might not be at fault; it might be one of the other areas in this list.");
                    out.add(new AreaGroup(Arrays.asList(area), osm));
                }
            }
        }
        return out;
    }

    public class RingConstructionException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private Ring toRing(Polygon polygon, Map<Coordinate, Long> nodeMap, OSM osm) {
        List<Long> shell = Lists.newArrayList();
        for (Coordinate coord : polygon.getExteriorRing().getCoordinates()) {
            Long nodeId = nodeMap.get(coord);
            if (nodeId == null) {
                throw new RingConstructionException();
            }
            shell.add(nodeId);
        }
        Ring ring = new Ring(shell, osm);
        // now the holes
        for (int i = 0; i < polygon.getNumInteriorRing(); ++i) {
            LineString interior = polygon.getInteriorRingN(i);
            List<Long> hole = Lists.newArrayList();
            for (Coordinate coord : interior.getCoordinates()) {
                Long nodeId = nodeMap.get(coord);
                if (nodeId == null) {
                    throw new RingConstructionException();
                }
                hole.add(nodeId);
            }
            ring.holes.add(new Ring(hole, osm));
        }

        return ring;
    }

    // FIXME result is not of a known type (way or relation)
    public long getSomeOSMObject() {
        return areas.iterator().next().parentId;
    }
}
