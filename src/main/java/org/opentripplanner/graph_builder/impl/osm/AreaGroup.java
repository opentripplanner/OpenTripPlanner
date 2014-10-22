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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

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
                allRings.add(ring.toJtsPolygon());
                for (OSMNode node : ring.nodes) {
                    nodeMap.put(new Coordinate(node.lon, node.lat), node);
                }
                for (Ring inner : ring.holes) {
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

    public class RingConstructionException extends RuntimeException {
        private static final long serialVersionUID = 1L;
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
        Ring ring = new Ring(shell, true);
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
            ring.holes.add(new Ring(hole, true));
        }

        return ring;
    }

    public OSMWithTags getSomeOSMObject() {
        return areas.iterator().next().parent;
    }
}
