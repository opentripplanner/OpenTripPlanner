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

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A generic indexed grid of Z samples.
 * 
 * @author laurent
 */
public final class SparseMatrixZSampleGrid<TZ> implements ZSampleGrid<TZ> {

    private final static class SparseMatrixSamplePoint<TZ> implements ZSamplePoint<TZ> {

        @Getter
        private int x;

        @Getter
        private int y;

        @Getter
        @Setter
        private TZ z;

        @Getter
        @Setter
        private Object userData;

        private SparseMatrixSamplePoint<TZ> up, down, right, left;

        @Override
        public ZSamplePoint<TZ> up() {
            return up;
        }

        @Override
        public ZSamplePoint<TZ> down() {
            return down;
        }

        @Override
        public ZSamplePoint<TZ> right() {
            return right;
        }

        @Override
        public ZSamplePoint<TZ> left() {
            return left;
        }
    }

    private double dX, dY;

    private Coordinate center;

    private SparseMatrix<SparseMatrixSamplePoint<TZ>> allSamples;

    /**
     * @param chunkSize SparseMatrix chunk side (eg 8 or 16). See SparseMatrix.
     * @param totalSize Total estimated size for pre-allocating.
     * @param dX X grid size, same units as center coordinates.
     * @param dY Y grid size, same units as center coordinates.
     * @param center Center position of the grid. Do not need to be precise.
     */
    public SparseMatrixZSampleGrid(int chunkSize, int totalSize, double dX, double dY,
            Coordinate center) {
        this.center = center;
        this.dX = dX;
        this.dY = dY;
        allSamples = new SparseMatrix<SparseMatrixSamplePoint<TZ>>(chunkSize, totalSize);
    }

    public ZSamplePoint<TZ> getOrCreate(int x, int y) {
        SparseMatrixSamplePoint<TZ> A = allSamples.get(x, y);
        if (A != null)
            return A;
        A = new SparseMatrixSamplePoint<TZ>();
        A.x = x;
        A.y = y;
        A.z = null;
        SparseMatrixSamplePoint<TZ> Aup = allSamples.get(x, y + 1);
        if (Aup != null) {
            Aup.down = A;
            A.up = Aup;
        }
        SparseMatrixSamplePoint<TZ> Adown = allSamples.get(x, y - 1);
        if (Adown != null) {
            Adown.up = A;
            A.down = Adown;
        }
        SparseMatrixSamplePoint<TZ> Aright = allSamples.get(x + 1, y);
        if (Aright != null) {
            Aright.left = A;
            A.right = Aright;
        }
        SparseMatrixSamplePoint<TZ> Aleft = allSamples.get(x - 1, y);
        if (Aleft != null) {
            Aleft.right = A;
            A.left = Aleft;
        }
        allSamples.put(x, y, A);
        return A;
    }

    @Override
    public Iterator<ZSamplePoint<TZ>> iterator() {
        return new Iterator<ZSamplePoint<TZ>>() {

            private Iterator<SparseMatrixSamplePoint<TZ>> iterator = allSamples.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public ZSamplePoint<TZ> next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    @Override
    public Coordinate getCoordinates(ZSamplePoint<TZ> point) {
        // TODO Cache the coordinates in the point?
        return new Coordinate(point.getX() * dX + center.x, point.getY() * dY + center.y);
    }

    @Override
    public int[] getLowerLeftIndex(Coordinate C) {
        return new int[] { (int) Math.round((C.x - center.x - dX / 2) / dX),
                (int) Math.round((C.y - center.y - dY / 2) / dY) };
    }

    @Override
    public int size() {
        return allSamples.size();
    }
}