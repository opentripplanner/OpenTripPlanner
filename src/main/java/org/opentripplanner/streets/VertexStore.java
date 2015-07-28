package org.opentripplanner.streets;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.io.Serializable;

/**
 *
 */
public class VertexStore implements Serializable {

    public int nVertices = 0;
    public static final double FIXED_FACTOR = 1e7; // we could just reuse the constant from osm-lib Node.
    public TIntList fixedLats;
    public TIntList fixedLons;

    public VertexStore (int initialSize) {
        fixedLats = new TIntArrayList(initialSize);
        fixedLons = new TIntArrayList(initialSize);
    }

    public int addVertex (double lat, double lon) {
        int vertexIndex = nVertices++;
        fixedLats.add((int)(lat * FIXED_FACTOR));
        fixedLons.add((int)(lon * FIXED_FACTOR));
        return vertexIndex;
    }

    public class Vertex {

        public int index;

        public Vertex (int index) {
            this.index = index;
        }

        public Vertex () {
            this (-1); // must call advance() before use.
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

    }

    public Vertex getCursor() {
        return new Vertex();
    }

    public Vertex getCursor(int index) {
        return new Vertex(index);
    }

}
