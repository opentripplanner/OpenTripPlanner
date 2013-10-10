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
import java.util.Map;

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
public class SparseMatrix<T> {

    /*
     * For lazyness and efficiency, we use an internal iterator. TODO Implement Iterable<T> ?
     */
    public interface Visitor<T> {
        void visit(T t);
    }

    private int shift;

    private int mask;

    private Map<Long, T[]> chunks;

    int size = 0;

    /**
     * @param chunkSize Chunk size, must be a power of two. Keep it small (8, 16, 32...). Number of
     *        elements in each matrix chunk will be the square of this value.
     * @param totalSize Estimated total size of the matrix.
     */
    public SparseMatrix(int chunkSize, int totalSize) {
        shift = 0;
        mask = chunkSize - 1;
        while (chunkSize > 1) {
            if (chunkSize % 2 != 0)
                throw new IllegalArgumentException("Chunk size must be a power of 2");
            chunkSize /= 2;
            shift++;
        }
        // We assume here that each chunk will be filled at ~25% (thus the x4)
        this.chunks = new HashMap<Long, T[]>(totalSize / ((mask + 1) * (mask + 1)) * 4);
    }

    public final T get(int x, int y) {
        x += 0x7FFFFFFF;
        y += 0x7FFFFFFF;
        long x0 = x >> shift;
        long y0 = y >> shift;
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
        x += 0x7FFFFFFF;
        y += 0x7FFFFFFF;
        long x0 = x >> shift;
        long y0 = y >> shift;
        Long key = x0 + (y0 << 32);
        T[] ts = chunks.get(key);
        if (ts == null) {
            // Java do not allow us to create an array of generics...
            ts = (T[]) (new Object[(mask + 1) * (mask + 1)]);
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

    public void iterate(Visitor<T> visitor) {
        for (T[] ts : chunks.values()) {
            for (int i = 0; i < ts.length; i++) {
                T t = ts[i];
                if (t != null) {
                    visitor.visit(t);
                }
            }
        }
    }

}
