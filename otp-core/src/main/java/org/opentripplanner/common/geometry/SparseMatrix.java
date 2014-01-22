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

import lombok.Getter;

/**
 * Implementation of a fast sparse 2D matrix of T elements.
 * 
 * We use a hash map of chunks of smaller chunkSize x chunkSize arrays. Do not implement any
 * collection interface for simplicity and to keep it fast.
 * 
 * Not thread-safe!
 * 
 * @author laurent
 */
public class SparseMatrix<T> implements Iterable<T> {

    /* The biggest negative 32 bits value that is a multiple of 128 */
    private static final long INDEX_OFFSET = 2147483520L;

    private int shift;

    private int mask;

    private Map<Long, T[]> chunks;

    int size = 0, matSize, chunkSize;

    @Getter
    int xMin, xMax, yMin, yMax;

    /**
     * @param chunkSize Chunk size, must be a power of two. Keep it small (8, 16, 32...). Number of
     *        elements in each matrix chunk will be the square of this value.
     * @param totalSize Estimated total size of the matrix.
     */
    public SparseMatrix(int chunkSize, int totalSize) {
        shift = 0;
        this.chunkSize = chunkSize;
        mask = chunkSize - 1;
        while (chunkSize > 1) {
            if (chunkSize % 2 != 0)
                throw new IllegalArgumentException("Chunk size must be a power of 2");
            chunkSize /= 2;
            shift++;
        }
        this.matSize = (mask + 1) * (mask + 1);
        // We assume here that each chunk will be filled at ~25% (thus the x4)
        this.chunks = new HashMap<Long, T[]>(totalSize / matSize * 4);
        this.xMin = Integer.MAX_VALUE;
        this.yMin = Integer.MAX_VALUE;
        this.xMax = Integer.MIN_VALUE;
        this.yMax = Integer.MIN_VALUE;
    }

    public final T get(int x, int y) {
        long x0 = ((long) x + INDEX_OFFSET) >> shift;
        long y0 = ((long) y + INDEX_OFFSET) >> shift;
        Long key = x0 + (y0 << 32);
        T[] ts = chunks.get(key);
        if (ts == null) {
            return null;
        }
        int index = ((x & mask) << shift) + (y & mask);
        return ts[index];
    }

    @SuppressWarnings("unchecked")
    public final T put(int x, int y, T t) {
        if (x < xMin)
            xMin = x;
        if (x > xMax)
            xMax = x;
        if (y < yMin)
            yMin = y;
        if (y > yMax)
            yMax = y;
        long x0 = ((long) x + INDEX_OFFSET) >> shift;
        long y0 = ((long) y + INDEX_OFFSET) >> shift;
        Long key = x0 + (y0 << 32);
        T[] ts = chunks.get(key);
        if (ts == null) {
            // Java do not allow us to create an array of generics...
            ts = (T[]) (new Object[matSize]);
            chunks.put(key, ts);
        }
        int index = ((x & mask) << shift) + (y & mask);
        if (ts[index] == null)
            size++;
        ts[index] = t;
        return t;
    }

    public int size() {
        return size;
    }

    public int getChunkSize() {
        return chunkSize;
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
     * A public representation of the internal structure of the sparse matrix, ie a map of chunks
     * (bi-dimentional array). We need to publish the internal structure to be able to efficiently
     * export it through web-services.
     */
    public class SparseMatrixChunk {

        @Getter
        public int x0, y0;

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
                final Iterator<Long> indexIterator = chunks.keySet().iterator();
                return new Iterator<SparseMatrixChunk>() {

                    @Override
                    public boolean hasNext() {
                        return indexIterator.hasNext();
                    }

                    @Override
                    public SparseMatrixChunk next() {
                        long index = indexIterator.next();
                        T[] ts = chunks.get(index);
                        if (ts == null) {
                            return null;
                        }
                        int x0 = (int) (((index & 0xFFFFFFFFL) << shift) - INDEX_OFFSET);
                        int y0 = (int) ((((index & 0xFFFFFFFF00000000L) >> 32) << shift) - INDEX_OFFSET);
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

}
