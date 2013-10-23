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

import java.util.Iterator;

import lombok.Getter;
import lombok.Setter;

/**
 * A generic indexed grid of Z samples.
 * 
 * @author laurent
 */
public final class SparseMatrixZSampleGrid<TZ, U> implements ZSampleGrid<TZ, U> {

    private final static class SparseMatrixSamplePoint<TZ, U> implements ZSamplePoint<TZ, U> {

        @Getter
        private int x;

        @Getter
        private int y;

        @Getter
        @Setter
        private TZ z;

        @Getter
        @Setter
        private U u;

        private SparseMatrixSamplePoint<TZ, U> up, down, right, left;

        @Override
        public ZSamplePoint<TZ, U> up() {
            return up;
        }

        @Override
        public ZSamplePoint<TZ, U> down() {
            return down;
        }

        @Override
        public ZSamplePoint<TZ, U> right() {
            return right;
        }

        @Override
        public ZSamplePoint<TZ, U> left() {
            return left;
        }
    }

    private SparseMatrix<SparseMatrixSamplePoint<TZ, U>> allSamples;

    public SparseMatrixZSampleGrid(int chunkSize, int totalSize) {
        allSamples = new SparseMatrix<SparseMatrixSamplePoint<TZ, U>>(chunkSize, totalSize);
    }

    public ZSamplePoint<TZ, U> getOrCreate(int x, int y) {
        SparseMatrixSamplePoint<TZ, U> A = allSamples.get(x, y);
        if (A != null)
            return A;
        A = new SparseMatrixSamplePoint<TZ, U>();
        A.x = x;
        A.y = y;
        A.z = null;
        SparseMatrixSamplePoint<TZ, U> Aup = allSamples.get(x, y + 1);
        if (Aup != null) {
            Aup.down = A;
            A.up = Aup;
        }
        SparseMatrixSamplePoint<TZ, U> Adown = allSamples.get(x, y - 1);
        if (Adown != null) {
            Adown.up = A;
            A.down = Adown;
        }
        SparseMatrixSamplePoint<TZ, U> Aright = allSamples.get(x + 1, y);
        if (Aright != null) {
            Aright.left = A;
            A.right = Aright;
        }
        SparseMatrixSamplePoint<TZ, U> Aleft = allSamples.get(x - 1, y);
        if (Aleft != null) {
            Aleft.right = A;
            A.left = Aleft;
        }
        allSamples.put(x, y, A);
        return A;
    }

    @Override
    public Iterator<ZSamplePoint<TZ, U>> iterator() {
        return new Iterator<ZSamplePoint<TZ, U>>() {

            private Iterator<SparseMatrixSamplePoint<TZ, U>> iterator = allSamples.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public ZSamplePoint<TZ, U> next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    @Override
    public int size() {
        return allSamples.size();
    }
}