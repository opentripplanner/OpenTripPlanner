package org.opentripplanner.streets;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.io.Serializable;

/**
 *
 */
public class VertexStore implements Serializable {

    // TODO direct mm_per_fixed_degree conversion, work entirely in mm and fixed degrees.

    public int nVertices = 0;
    public static final double FIXED_FACTOR = 1e7; // we could just reuse the constant from osm-lib Node.
    public TIntList fixedLats;
    public TIntList fixedLons;

    public VertexStore (int initialSize) {
        fixedLats = new TIntArrayList(initialSize);
        fixedLons = new TIntArrayList(initialSize);
    }

    /**
     * Add a vertex, specifying its coordinates in double-precision floating point degrees.
     * @return the index of the new vertex.
     */
    public int addVertex (double lat, double lon) {
        return addVertexFixed(floatingDegreesToFixed(lat), floatingDegreesToFixed(lon));
    }

    /**
     * Add a vertex, specifying its coordinates in fixed-point lat and lon.
     * @return the index of the new vertex.
     */
    public int addVertexFixed (int fixedLat, int fixedLon) {
        int vertexIndex = nVertices++;
        fixedLats.add(fixedLat);
        fixedLons.add(fixedLon);
        return vertexIndex;
    }

    public class Vertex {

        public int index;

        /** Must call advance() before use, e.g. while (vertex.advance()) {...} */
        public Vertex () {
            this (-1);
        }

        public Vertex (int index) {
            this.index = index;
        }

        /** @return whether this cursor is still within the list (there is a vertex to read). */
        public boolean advance () {
            index += 1;
            return index < nVertices;
        }

        public void seek (int index) {
            this.index = index;
        }

        public void setLat(double lat) {
            fixedLats.set(index, (int)(lat * FIXED_FACTOR));
        }

        public void setLon(double lon) {
            fixedLons.set(index, (int)(lon * FIXED_FACTOR));
        }

        public void setLatLon(double lat, double lon) {
            setLat(lat);
            setLon(lon);
        }

        public double getLat() {
            return fixedLats.get(index) / FIXED_FACTOR;
        }

        public double getLon() {
            return fixedLons.get(index) / FIXED_FACTOR;
        }

        public int getFixedLat() {
            return fixedLats.get(index);
        }

        public int getFixedLon() {
            return fixedLons.get(index);
        }

    }

    public Vertex getCursor() {
        return new Vertex();
    }

    public Vertex getCursor(int index) {
        return new Vertex(index);
    }

    public static int floatingDegreesToFixed(double degrees) {
        return (int)(degrees * FIXED_FACTOR);
    }

    public static double fixedDegreesToFloating(int fixed) {
        return fixed / FIXED_FACTOR;
    }

}
