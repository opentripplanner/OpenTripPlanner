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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A generic indexed grid of Z samples.
 * 
 * Internally use a SparseMatrix to store samples.
 * 
 * @author laurent
 */
public final class SparseMatrixZSampleGrid<TZ> implements ZSampleGrid<TZ>,
        DelaunayTriangulation<TZ> {

    private final class SparseMatrixSamplePoint implements ZSamplePoint<TZ>, DelaunayPoint<TZ> {

        private int x;

        private int y;

        private TZ z;

        private SparseMatrixSamplePoint up, down, right, left;

        private GridDelaunayEdge eUp, eUpRight, eRight;

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

        @Override
        public Coordinate getCoordinates() {
            return SparseMatrixZSampleGrid.this.getCoordinates(this);
        }

		@Override
		public int getX() {
			return this.x;
		}

		@Override
		public int getY() {
			return this.y;
		}

		@Override
		public TZ getZ() {
			return this.z;
		}

		@Override
		public void setZ(TZ z) {
			this.z = z;
		}
    }

    private final class GridDelaunayEdge implements DelaunayEdge<TZ> {

        private static final int TYPE_VERTICAL = 0;

        private static final int TYPE_HORIZONTAL = 1;

        private static final int TYPE_DIAGONAL = 2;

        private boolean processed;

        private SparseMatrixSamplePoint A, B;

        private GridDelaunayEdge ccw1, ccw2, cw1, cw2;

        private int type;

        private GridDelaunayEdge(SparseMatrixSamplePoint A, SparseMatrixSamplePoint B, int type) {
            this.A = A;
            this.B = B;
            switch (type) {
            case TYPE_HORIZONTAL:
                A.eRight = this;
                break;
            case TYPE_VERTICAL:
                A.eUp = this;
                break;
            case TYPE_DIAGONAL:
                A.eUpRight = this;
                break;
            }
            this.type = type;
        }

        @Override
        public DelaunayPoint<TZ> getA() {
            return A;
        }

        @Override
        public DelaunayPoint<TZ> getB() {
            return B;
        }

        @Override
        public DelaunayEdge<TZ> getEdge1(boolean ccw) {
            return ccw ? ccw1 : cw1;
        }

        @Override
        public DelaunayEdge<TZ> getEdge2(boolean ccw) {
            return ccw ? ccw2 : cw2;
        }

        @Override
        public boolean isProcessed() {
            return processed;
        }

        @Override
        public void setProcessed(boolean processed) {
            this.processed = processed;
        }

        @Override
        public String toString() {
            return "<GridDelaunayEdge " + A.getCoordinates() + "->" + B.getCoordinates() + ">";
        }

    }

    private double dX, dY;

    private Coordinate center;

    @SuppressWarnings("unused")
    private int chunkSize;

    private SparseMatrix<SparseMatrixSamplePoint> allSamples;

    private List<GridDelaunayEdge> triangulation = null;

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
        this.chunkSize = chunkSize;
        allSamples = new SparseMatrix<SparseMatrixSamplePoint>(chunkSize, totalSize);
    }

    public ZSamplePoint<TZ> getOrCreate(int x, int y) {
        SparseMatrixSamplePoint A = allSamples.get(x, y);
        if (A != null)
            return A;
        A = new SparseMatrixSamplePoint();
        A.x = x;
        A.y = y;
        A.z = null;
        SparseMatrixSamplePoint Aup = allSamples.get(x, y + 1);
        if (Aup != null) {
            Aup.down = A;
            A.up = Aup;
        }
        SparseMatrixSamplePoint Adown = allSamples.get(x, y - 1);
        if (Adown != null) {
            Adown.up = A;
            A.down = Adown;
        }
        SparseMatrixSamplePoint Aright = allSamples.get(x + 1, y);
        if (Aright != null) {
            Aright.left = A;
            A.right = Aright;
        }
        SparseMatrixSamplePoint Aleft = allSamples.get(x - 1, y);
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

            private Iterator<SparseMatrixSamplePoint> iterator = allSamples.iterator();

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
    public Coordinate getCenter() {
        return center;
    }
    
    @Override
    public Coordinate getCellSize() {
        return new Coordinate(dX, dY);
    }

    @Override
    public int getXMin() {
        return allSamples.xMin;
    }

    @Override
    public int getXMax() {
        return allSamples.xMax;
    }

    @Override
    public int getYMin() {
        return allSamples.yMin;
    }

    @Override
    public int getYMax() {
        return allSamples.yMax;
    }
    
    @Override
    public int size() {
        return allSamples.size();
    }

    @Override
    public int edgesCount() {
        if (triangulation == null) {
            delaunify();
        }
        return triangulation.size();
    }

    @Override
    public Iterable<? extends DelaunayEdge<TZ>> edges() {
        if (triangulation == null) {
            delaunify();
        }
        return triangulation;
    }

    /**
     * The conversion from a grid of points to a Delaunay triangulation is trivial. Each square from
     * the grid is cut through one diagonal in two triangles, the resulting output is a Delaunay
     * triangulation.
     */
    private void delaunify() {
        triangulation = new ArrayList<GridDelaunayEdge>(allSamples.size() * 3);
        // 1. Create unlinked edges
        for (SparseMatrixSamplePoint A : allSamples) {
            SparseMatrixSamplePoint B = (SparseMatrixSamplePoint) A.right();
            SparseMatrixSamplePoint D = (SparseMatrixSamplePoint) A.up();
            SparseMatrixSamplePoint C = (SparseMatrixSamplePoint) (B != null ? B.up()
                    : D != null ? D.right() : null);
            if (B != null)
                triangulation.add(new GridDelaunayEdge(A, B, GridDelaunayEdge.TYPE_HORIZONTAL));
            if (D != null)
                triangulation.add(new GridDelaunayEdge(A, D, GridDelaunayEdge.TYPE_VERTICAL));
            if (C != null)
                triangulation.add(new GridDelaunayEdge(A, C, GridDelaunayEdge.TYPE_DIAGONAL));
        }
        // 2. Link edges
        for (GridDelaunayEdge e : triangulation) {
            switch (e.type) {
            case GridDelaunayEdge.TYPE_HORIZONTAL:
                e.ccw1 = e.B.eUp;
                e.ccw2 = e.A.eUpRight;
                e.cw1 = e.A.down == null ? null : e.A.down.eUpRight;
                e.cw2 = e.A.down == null ? null : e.A.down.eUp;
                break;
            case GridDelaunayEdge.TYPE_VERTICAL:
                e.ccw1 = e.A.left == null ? null : e.A.left.eUpRight;
                e.ccw2 = e.A.left == null ? null : e.A.left.eRight;
                e.cw1 = e.B.eRight;
                e.cw2 = e.A.eUpRight;
                break;
            case GridDelaunayEdge.TYPE_DIAGONAL:
                e.ccw1 = e.A.up == null ? null : e.A.up.eRight;
                e.ccw2 = e.A.eUp;
                e.cw1 = e.A.right == null ? null : e.A.right.eUp;
                e.cw2 = e.A.eRight;
                break;
            }
        }
    }

    @Override
    public DelaunayTriangulation<TZ> delaunayTriangulate() {
        // We ourselves are a DelaunayTriangulation
        return this;
    }
}