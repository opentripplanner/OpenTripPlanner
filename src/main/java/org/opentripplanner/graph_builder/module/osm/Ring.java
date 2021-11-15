package org.opentripplanner.graph_builder.module.osm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import gnu.trove.list.TLongList;
import gnu.trove.map.TLongObjectMap;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.openstreetmap.model.OSMNode;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

public class Ring {

    public static class RingConstructionException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public List<OSMNode> nodes;

    public Coordinate[] geometry;

    public List<Ring> holes = new ArrayList<Ring>();

    // equivalent to the ring representation, but used for JTS operations
    private Polygon jtsPolygon;

    public Ring(List<OSMNode> osmNodes) {
        ArrayList<Coordinate> vertices = new ArrayList<>();
        nodes = osmNodes;
        for (OSMNode node : osmNodes) {
            Coordinate point = new Coordinate(node.lon, node.lat);
            vertices.add(point);
        }
        geometry = vertices.toArray(new Coordinate[0]);
    }

    public Ring(TLongList osmNodes, TLongObjectMap<OSMNode> _nodes) {
        ArrayList<Coordinate> vertices = new ArrayList<>();
        nodes = new ArrayList<>(osmNodes.size());
        osmNodes.forEach(nodeId -> {
            OSMNode node = _nodes.get(nodeId);
            if (nodes.contains(node)) {
                // Hopefully, this only happens in order to close polygons. Next iteration.
                return true;
            }
            Coordinate point = new Coordinate(node.lon, node.lat);
            nodes.add(node);
            vertices.add(point);
            return true;
        });
        geometry = vertices.toArray(new Coordinate[0]);
    }

    public Polygon toJtsPolygon() {
        if (jtsPolygon != null) {
            return jtsPolygon;
        }
        GeometryFactory factory = GeometryUtils.getGeometryFactory();

        LinearRing shell;
        try {
            shell = factory.createLinearRing(geometry);
        } catch (IllegalArgumentException e) {
            throw new RingConstructionException();
        }

        // we need to merge connected holes here, because JTS does not believe in
        // holes that touch at multiple points (and, weirdly, does not have a method
        // to detect this other than this crazy DE-9IM stuff

        List<Polygon> polygonHoles = new ArrayList<Polygon>();
        for (Ring ring : holes) {
            LinearRing linearRing = factory.createLinearRing(ring.geometry);
            Polygon polygon = factory.createPolygon(linearRing, new LinearRing[0]);
            for (Iterator<Polygon> it = polygonHoles.iterator(); it.hasNext();) {
                Polygon otherHole = it.next();
                if (otherHole.relate(polygon, "F***1****")) {
                    polygon = (Polygon) polygon.union(otherHole);
                    it.remove();
                }
            }
            polygonHoles.add(polygon);
        }

        ArrayList<LinearRing> lrholelist = new ArrayList<LinearRing>(polygonHoles.size());

        for (Polygon hole : polygonHoles) {
            Geometry boundary = hole.getBoundary();
            if (boundary instanceof LinearRing) {
                lrholelist.add((LinearRing) boundary);
            } else {
                // this is a case of a hole inside a hole. OSM technically
                // allows this, but it would be a giant hassle to get right. So:
                LineString line = hole.getExteriorRing();
                LinearRing ring = factory.createLinearRing(line.getCoordinates());
                lrholelist.add(ring);
            }
        }
        LinearRing[] lrholes = lrholelist.toArray(new LinearRing[lrholelist.size()]);
        jtsPolygon = factory.createPolygon(shell, lrholes);
        return jtsPolygon;
    }

}
