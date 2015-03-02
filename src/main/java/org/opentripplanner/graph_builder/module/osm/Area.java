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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.primitives.Longs;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.osm.OSM;
import org.opentripplanner.osm.Tagged;
import org.opentripplanner.osm.Way;

import java.util.*;

/**
 * Stores information about an OSM area needed for visibility graph construction. Algorithm based on
 * http://wiki.openstreetmap.org/wiki/Relation:multipolygon/Algorithm but generally done in a
 * quick/dirty way.
 *
 * TODO store source and its ID as string (since it can be way or relation) and store Level directly in the Area
 * parent field is not only used in error messages?
 */
class Area {

    public class AreaConstructionException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    // The way or relation from which this Area was created, and which holds the relevant tags for this Area
    Tagged parent;

    long parentId; // TODO set me

    /** @return a string describing the class and identifier of this area's parent entity, for use in error messages. */
    public String describeParent () {
        return parent.getClass().getSimpleName() + " " + parentId;
    }

    List<Ring> outermostRings = new ArrayList<Ring>();

    private MultiPolygon jtsMultiPolygon;

    /** Construct a new Area */
    Area(Tagged parent, List<Long> outerRingWays, List<Long> innerRingWays, OSM osm) {
        this.parent = parent;
        // ring assignment
        List<List<Long>> innerRingNodes = constructRings(innerRingWays, osm);
        List<List<Long>> outerRingNodes = constructRings(outerRingWays, osm);
        if (innerRingNodes == null || outerRingNodes == null) {
            throw new AreaConstructionException();
        }
        ArrayList<List<Long>> allRings = new ArrayList<List<Long>>(innerRingNodes);
        allRings.addAll(outerRingNodes);

        List<Ring> innerRings = new ArrayList<Ring>();
        List<Ring> outerRings = new ArrayList<Ring>();
        for (List<Long> ring : innerRingNodes) {
            innerRings.add(new Ring(ring, osm));
        }
        for (List<Long> ring : outerRingNodes) {
            outerRings.add(new Ring(ring, osm));
        }

        // now, ring grouping
        // first, find outermost rings
        OUTER: for (Ring outer : outerRings) {
            for (Ring possibleContainer : outerRings) {
                if (outer != possibleContainer
                        && outer.geometry.hasPointInside(possibleContainer.geometry)) {
                    continue OUTER;
                }
            }
            outermostRings.add(outer);

            // find holes in this ring
            for (Ring possibleHole : innerRings) {
                if (possibleHole.geometry.hasPointInside(outer.geometry)) {
                    outer.holes.add(possibleHole);
                }
            }
        }
        // run this at end of ctor so that exception
        // can be caught in the right place
        toJTSMultiPolygon();
    }

    public MultiPolygon toJTSMultiPolygon() {
        if (jtsMultiPolygon == null) {
            List<Polygon> polygons = new ArrayList<Polygon>();
            for (Ring ring : outermostRings) {
                polygons.add(ring.toJtsPolygon());
            }
            jtsMultiPolygon = GeometryUtils.getGeometryFactory().createMultiPolygon(
                    polygons.toArray(new Polygon[0]));
            if (!jtsMultiPolygon.isValid()) {
                throw new AreaConstructionException();
            }
        }

        return jtsMultiPolygon;
    }

    // TODO in Java 8 use TLongList.foreach()
    // TODO More documentation!
    public List<List<Long>> constructRings(List<Long> wayIds, OSM osm) {
        if (wayIds.size() == 0) {
            // no rings is no rings
            return Collections.emptyList();
        }

        List<List<Long>> closedRings = Lists.newArrayList();
        ArrayListMultimap<Long, Long> waysByEndpointNode = ArrayListMultimap.create();

        for (long wayId : wayIds) {
            Way way = osm.ways.get(wayId);
            long[] refs = way.nodes;

            long start = way.nodes[0];
            long end = way.nodes[way.nodes.length - 1];
            if (start == end) {
                List<Long> ring = Longs.asList(way.nodes); // fixed length, not a copy...
                closedRings.add(ring);
            } else {
                waysByEndpointNode.put(start, wayId);
                waysByEndpointNode.put(end, wayId);
            }
        }

        // precheck for impossible situations
        List<Long> toRemove = new ArrayList<Long>();
        for (Long endpoint : waysByEndpointNode.keySet()) {
            Collection<Long> list = waysByEndpointNode.get(endpoint);
            if (list.size() % 2 == 1) {
                return null;
            }
        }
        for (Long key : toRemove) {
            waysByEndpointNode.removeAll(key);
        }

        List<Long> partialRing = new ArrayList<Long>();
        if (waysByEndpointNode.size() == 0) {
            return closedRings;
        }

        long firstEndpoint = 0, otherEndpoint = 0;
        long firstWayId = 0;
        for (Long endpoint : waysByEndpointNode.keySet()) {
            firstWayId = waysByEndpointNode.get(endpoint).get(0);
            Way firstWay = osm.ways.get(firstWayId);
            partialRing.addAll(Longs.asList(firstWay.nodes));
            firstEndpoint = firstWay.nodes[0];
            otherEndpoint = firstWay.nodes[firstWay.nodes.length - 1];
            break;
        }
        waysByEndpointNode.get(firstEndpoint).remove(firstWayId);
        waysByEndpointNode.get(otherEndpoint).remove(firstWayId);
        if (constructRingsRecursive(waysByEndpointNode, partialRing, closedRings, firstEndpoint, osm)) {
            return closedRings;
        } else {
            return null;
        }
    }

    // TODO in Java 8 use TLongList.foreach()
    // TODO More documentation!
    private boolean constructRingsRecursive(ArrayListMultimap<Long, Long> waysByEndpointNode,
            List<Long> ring, List<List<Long>> closedRings, long endpointNodeId, OSM osm) {

        List<Long> ways = Lists.newArrayList(waysByEndpointNode.get(endpointNodeId));

        for (long wayId : ways) {
            // remove this way from the map
            Way way = osm.ways.get(wayId);
            List<Long> nodeRefs = Longs.asList(way.nodes);
            long firstEndpoint = nodeRefs.get(0);
            long otherEndpoint = nodeRefs.get(nodeRefs.size() - 1);

            waysByEndpointNode.remove(firstEndpoint, way);
            waysByEndpointNode.remove(otherEndpoint, way);

            ArrayList<Long> newRing = new ArrayList<Long>(ring.size() + nodeRefs.size());
            long newFirstEndpoint;
            if (firstEndpoint == endpointNodeId) {
                for (int j = nodeRefs.size() - 1; j >= 1; --j) {
                    newRing.add(nodeRefs.get(j));
                }
                newRing.addAll(ring);
                newFirstEndpoint = otherEndpoint;
            } else {
                newRing.addAll(nodeRefs.subList(0, nodeRefs.size() - 1));
                newRing.addAll(ring);
                newFirstEndpoint = firstEndpoint;
            }
            if (newRing.get(newRing.size() - 1).equals(newRing.get(0))) {
                // ring closure
                closedRings.add(newRing);
                // if we're out of endpoints, then we have succeeded
                if (waysByEndpointNode.size() == 0) {
                    return true; // success
                }

                // otherwise, we need to start a new partial ring
                newRing = new ArrayList<Long>();
                long firstWayId = 0;
                Way firstWay = null;
                for (Long entry : waysByEndpointNode.keySet()) {
                    List<Long> list = waysByEndpointNode.get(entry);
                    firstWayId = list.get(0);
                    firstWay = osm.ways.get(firstWayId);
                    nodeRefs = Longs.asList(firstWay.nodes);
                    newRing.addAll(nodeRefs);
                    firstEndpoint = nodeRefs.get(0);
                    otherEndpoint = nodeRefs.get(nodeRefs.size() - 1);
                    break;
                }

                waysByEndpointNode.remove(firstEndpoint, firstWay);
                waysByEndpointNode.remove(otherEndpoint, firstWay);

                if (constructRingsRecursive(waysByEndpointNode, newRing, closedRings, firstEndpoint, osm)) {
                    return true;
                }

                waysByEndpointNode.remove(firstEndpoint, firstWay);
                waysByEndpointNode.remove(otherEndpoint, firstWay);

            } else {
                // continue with this ring
                if (waysByEndpointNode.get(newFirstEndpoint) != null) {
                    if (constructRingsRecursive(waysByEndpointNode, newRing, closedRings, newFirstEndpoint, osm)) {
                        return true;
                    }
                }
            }
            if (firstEndpoint == endpointNodeId) {
                waysByEndpointNode.put(otherEndpoint, wayId);
            } else {
                waysByEndpointNode.put(firstEndpoint, wayId);
            }
        }
        return false;
    }
}
