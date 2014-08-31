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

package org.opentripplanner.common.geometry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A fast sparse 2D matrix holding elements of type T.
 * The x and y indexes into the sparse matrix are _signed_ 32-bit integers (negative indexes are allowed).
 * Square sub-chunks of size chunkSize x chunkSize are stored in a hashmap,
 * keyed on a combination of the x and y coordinates.
 * Does not implement the collection interface for simplicity and speed.
 * Not thread-safe!
 * 
 * @author laurent
 */
public class SparseMatrix<T> implements Iterable<T> {

    private int shift; // How many low order bits to shift off to get the index of a chunk.

    private int mask; // The low order bits to retain when finding the index within a chunk.

    private Map<Key, T[]> chunks;

    int size = 0; // The number of elements currently stored in this matrix (number of cells containing a T).

    int matSize; // The capacity of a single chunk TODO rename

    int chunkSize; // The dimension of a single chunk in each of two dimensions TODO rename

    public int xMin, xMax, yMin, yMax; // The maximum and minimum indices where an element is stored.

    /**
     * @param chunkSize Must be a power of two so chunk indexes can be determined by shifting off low order bits.
     *        Keep it small (8, 16, 32...). Chunks are square, with this many elements in each of two dimensions,
     *        so the number of elements in each chunk will be the square of this value.
     * @param totalSize Estimated total number of elements to be stored in the matrix (actual use, not capacity).
     */
    public SparseMatrix(int chunkSize, int totalSize) {
        shift = 0;
        this.chunkSize = chunkSize;
        mask = chunkSize - 1; // all low order bits below the given power of two
        this.matSize = chunkSize * chunkSize; // capacity of a single chunk
        /* Find log_2 chunkSize, the number of low order bits to shift off an index to get its chunk index. */
        while (chunkSize > 1) {
            if (chunkSize % 2 != 0)
                throw new IllegalArgumentException("Chunk size must be a power of 2");
            chunkSize /= 2;
            shift++;
        }
        // We assume here that each chunk will be filled at ~25% (thus the x4)
        this.chunks = new HashMap<Key, T[]>(totalSize / matSize * 4);
        this.xMin = Integer.MAX_VALUE;
        this.yMin = Integer.MAX_VALUE;
        this.xMax = Integer.MIN_VALUE;
        this.yMax = Integer.MIN_VALUE;
    }

    public final T get(int x, int y) {
        T[] ts = chunks.get(new Key(x, y, shift));
        if (ts == null) {
            return null;
        }
        int index = ((x & mask) << shift) + (y & mask);
        return ts[index];
    }

    @SuppressWarnings("unchecked")
    public final T put(int x, int y, T t) {
        /* Keep a bounding box around all matrix cells in use. */
        if (x < xMin)
            xMin = x;
        if (x > xMax)
            xMax = x;
        if (y < yMin)
            yMin = y;
        if (y > yMax)
            yMax = y;
        Key key = new Key(x, y, shift);
        T[] ts = chunks.get(key);
        if (ts == null) {
            ts = (T[]) (new Object[matSize]); // Java does not allow arrays of generics.
            chunks.put(key, ts);
        }
        /* Find index within chunk: concatenated low order bits of x and y. */
        int index = ((x & mask) << shift) + (y & mask);
        if (ts[index] == null)
            size++;
        ts[index] = t;
        return t;
    }

    public int size() {
        return size;
    }

    /*
     * We rely on the map iterator for checking for concurrent modification exceptions.
     */
    private class SparseMatrixIterator implements Iterator<T> {

        private Iterator<T[]> mapIterator;

        private int chunkIndex = -1;

        private T[] chunk = null;

        private SparseMatrixIterator() {
            mapIterator = chunks.values().iterator();
            moveToNext();
        }

        @Override
        public boolean hasNext() {
            boolean hasNext = chunk != null;
            return hasNext;
        }

        @Override
        public T next() {
            T t = chunk[chunkIndex];
            moveToNext();
            return t;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        private void moveToNext() {
            if (chunk == null) {
                chunk = mapIterator.hasNext() ? mapIterator.next() : null;
                if (chunk == null)
                    return; // End
            }
            while (true) {
                chunkIndex++;
                if (chunkIndex == matSize) {
                    chunkIndex = 0;
                    chunk = mapIterator.hasNext() ? mapIterator.next() : null;
                    if (chunk == null)
                        return; // End
                }
                if (chunk[chunkIndex] != null)
                    return;
            }
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new SparseMatrixIterator();
    }

    /**
     * A public representation of the internal structure of the sparse matrix, i.e. a map of chunks
     * (each of which is a flattened two-dimensional array).
     * We need to publish the internal structure to be able to efficiently export it through web-services.
     */
    public class SparseMatrixChunk {

        public int x0, y0; // the coordinates of the minimum corner of this chunk (i.e. with the low bits)

        public T[] ts;

        private SparseMatrixChunk(int x0, int y0, T[] ts) {
            this.x0 = x0;
            this.y0 = y0;
            this.ts = ts;
        }

        public T getT(int x, int y) {
            return ts[x * chunkSize + y];
        }
    }

    public Iterable<SparseMatrixChunk> getChunks() {
        return new Iterable<SparseMatrixChunk>() {
            @Override
            public Iterator<SparseMatrixChunk> iterator() {
                // Again, we rely on the indexIterator to check for concurrent modification.
                final Iterator<Key> keyIterator = chunks.keySet().iterator();
                return new Iterator<SparseMatrixChunk>() {

                    @Override
                    public boolean hasNext() {
                        return keyIterator.hasNext();
                    }

                    @Override
                    public SparseMatrixChunk next() {
                        Key key = keyIterator.next();
                        T[] ts = chunks.get(key);
                        if (ts == null) {
                            return null;
                        }
                        // extract the indexes for the minimum corner of this chunk
                        int x0 = key.x << shift;
                        int y0 = key.y << shift;
                        return new SparseMatrixChunk(x0, y0, ts);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("remove");
                    }
                };
            }
        };
    }

    /**
     * We were previously bit-shifting two 32 bit integers into a long. These are used as map keys, so they had to be
     * Long objects rather than primitive long ints. This purpose-built key object should be roughly the same in terms
     * of space and speed, and more readable.
     */
    static class Key {
        int x, y;
        public Key(int x, int y, int shift) {
            this.x = x >>> shift; // shift off low order bits (index within chunk) retaining only the chunk number
            this.y = y >>> shift; // same for y coordinate
        }
        @Override public int hashCode() {
            return x ^ y;
        }
        @Override public boolean equals(Object other) {
            return other instanceof Key && ((Key)other).x == x && ((Key)other).y == y;
        }
    }

}
