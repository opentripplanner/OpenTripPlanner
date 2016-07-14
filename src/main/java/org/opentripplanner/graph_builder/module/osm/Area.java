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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

import com.google.common.collect.ArrayListMultimap;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Stores information about an OSM area needed for visibility graph construction. Algorithm based on
 * http://wiki.openstreetmap.org/wiki/Relation:multipolygon/Algorithm but generally done in a
 * quick/dirty way.
 */
class Area {

    public static class AreaConstructionException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    // This is the way or relation that has the relevant tags for the area
    OSMWithTags parent;

    List<Ring> outermostRings = new ArrayList<Ring>();

    private MultiPolygon jtsMultiPolygon;

    Area(OSMWithTags parent, List<OSMWay> outerRingWays, List<OSMWay> innerRingWays,
            Map<Long, OSMNode> _nodes) {
        this.parent = parent;
        // ring assignment
        List<List<Long>> innerRingNodes = constructRings(innerRingWays);
        List<List<Long>> outerRingNodes = constructRings(outerRingWays);
        if (innerRingNodes == null || outerRingNodes == null) {
            throw new AreaConstructionException();
        }
        ArrayList<List<Long>> allRings = new ArrayList<List<Long>>(innerRingNodes);
        allRings.addAll(outerRingNodes);

        List<Ring> innerRings = new ArrayList<Ring>();
        List<Ring> outerRings = new ArrayList<Ring>();
        for (List<Long> ring : innerRingNodes) {
            innerRings.add(new Ring(ring, _nodes));
        }
        for (List<Long> ring : outerRingNodes) {
            outerRings.add(new Ring(ring, _nodes));
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

    public List<List<Long>> constructRings(List<OSMWay> ways) {
        if (ways.size() == 0) {
            // no rings is no rings
            return Collections.emptyList();
        }

        List<List<Long>> closedRings = new ArrayList<List<Long>>();

        ArrayListMultimap<Long, OSMWay> waysByEndpoint = ArrayListMultimap.create();
        for (OSMWay way : ways) {
            List<Long> refs = way.getNodeRefs();

            long start = refs.get(0);
            long end = refs.get(refs.size() - 1);
            if (start == end) {
                ArrayList<Long> ring = new ArrayList<Long>(refs);
                closedRings.add(ring);
            } else {
                waysByEndpoint.put(start, way);
                waysByEndpoint.put(end, way);
            }
        }

        // precheck for impossible situations
        List<Long> toRemove = new ArrayList<Long>();
        for (Long endpoint : waysByEndpoint.keySet()) {
            Collection<OSMWay> list = waysByEndpoint.get(endpoint);
            if (list.size() % 2 == 1) {
                return null;
            }
        }
        for (Long key : toRemove) {
            waysByEndpoint.removeAll(key);
        }

        List<Long> partialRing = new ArrayList<Long>();
        if (waysByEndpoint.size() == 0) {
            return closedRings;
        }

        long firstEndpoint = 0, otherEndpoint = 0;
        OSMWay firstWay = null;
        for (Long endpoint : waysByEndpoint.keySet()) {
            List<OSMWay> list = waysByEndpoint.get(endpoint);
            firstWay = list.get(0);
            List<Long> nodeRefs = firstWay.getNodeRefs();
            partialRing.addAll(nodeRefs);
            firstEndpoint = nodeRefs.get(0);
            otherEndpoint = nodeRefs.get(nodeRefs.size() - 1);
            break;
        }
        waysByEndpoint.get(firstEndpoint).remove(firstWay);
        waysByEndpoint.get(otherEndpoint).remove(firstWay);
        if (constructRingsRecursive(waysByEndpoint, partialRing, closedRings, firstEndpoint)) {
            return closedRings;
        } else {
            return null;
        }
    }

    private boolean constructRingsRecursive(ArrayListMultimap<Long, OSMWay> waysByEndpoint,
            List<Long> ring, List<List<Long>> closedRings, long endpoint) {

        List<OSMWay> ways = new ArrayList<OSMWay>(waysByEndpoint.get(endpoint));

        for (OSMWay way : ways) {
            // remove this way from the map
            List<Long> nodeRefs = way.getNodeRefs();
            long firstEndpoint = nodeRefs.get(0);
            long otherEndpoint = nodeRefs.get(nodeRefs.size() - 1);

            waysByEndpoint.remove(firstEndpoint, way);
            waysByEndpoint.remove(otherEndpoint, way);

            ArrayList<Long> newRing = new ArrayList<Long>(ring.size() + nodeRefs.size());
            long newFirstEndpoint;
            if (firstEndpoint == endpoint) {
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
                if (waysByEndpoint.size() == 0) {
                    return true; // success
                }

                // otherwise, we need to start a new partial ring
                newRing = new ArrayList<Long>();
                OSMWay firstWay = null;
                for (Long entry : waysByEndpoint.keySet()) {
                    List<OSMWay> list = waysByEndpoint.get(entry);
                    firstWay = list.get(0);
                    nodeRefs = firstWay.getNodeRefs();
                    newRing.addAll(nodeRefs);
                    firstEndpoint = nodeRefs.get(0);
                    otherEndpoint = nodeRefs.get(nodeRefs.size() - 1);
                    break;
                }

                waysByEndpoint.remove(firstEndpoint, firstWay);
                waysByEndpoint.remove(otherEndpoint, firstWay);

                if (constructRingsRecursive(waysByEndpoint, newRing, closedRings, firstEndpoint)) {
                    return true;
                }

                waysByEndpoint.remove(firstEndpoint, firstWay);
                waysByEndpoint.remove(otherEndpoint, firstWay);

            } else {
                // continue with this ring
                if (waysByEndpoint.get(newFirstEndpoint) != null) {
                    if (constructRingsRecursive(waysByEndpoint, newRing, closedRings,
                            newFirstEndpoint)) {
                        return true;
                    }
                }
            }
            if (firstEndpoint == endpoint) {
                waysByEndpoint.put(otherEndpoint, way);
            } else {
                waysByEndpoint.put(firstEndpoint, way);
            }
        }
        return false;
    }
}
