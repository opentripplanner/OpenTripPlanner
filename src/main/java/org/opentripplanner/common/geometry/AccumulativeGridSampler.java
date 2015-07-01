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
import java.util.List;

import org.opentripplanner.common.geometry.ZSampleGrid.ZSamplePoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Helper class to fill-in a ZSampleGrid from a given loosely-defined set of sampling points.
 * 
 * The process is customized by an "accumulative" metric which gives the behavior of cumulating
 * several values onto one sampling point.
 * 
 * To use this class, create an instance giving an AccumulativeMetric implementation as parameter.
 * Then for each source sample, call "addSample" with the its TZ value. At the end, call close() to
 * close the sample grid (ie add grid node at the edge to make sure borders are correctly defined,
 * the definition of correct is left to the client).
 * 
 * @author laurent
 */
public class AccumulativeGridSampler<TZ> {

    /**
     * An accumulative metric give the behavior of combining several samples to a regular sample
     * grid, ie how we should weight and add several TZ values from inside a cell to compute the
     * cell corner TZ values.
     * 
     * @author laurent
     * @param <TZ>
     */
    public interface AccumulativeMetric<TZ> {
        /**
         * Callback function to handle a new added sample.
         * 
         * @param C0 The initial position of the sample, as given in the addSample() call.
         * @param Cs The position of the sample on the grid, never farther away than (dX,dY)
         * @param z The z value of the initial sample, as given in the addSample() call.
         * @param zS The previous z value of the sample. Can be null if this is the first time, it's
         *        up to the caller to initialize the z value.
         * @param offRoadSpeed The offroad speed to assume.
         * @return The modified z value for the sample.
         */
        public TZ cumulateSample(Coordinate C0, Coordinate Cs, TZ z, TZ zS, double offRoadSpeed);

        /**
         * Callback function to handle a "closing" sample (that is a sample post-created to surround
         * existing samples and provide nice and smooth edges for the algorithm).
         * 
         * @param point The point to set Z.
         * @return True if the point "close" the set.
         */
        public boolean closeSample(ZSamplePoint<TZ> point);
    }

    private static final Logger LOG = LoggerFactory.getLogger(AccumulativeGridSampler.class);

    private AccumulativeMetric<TZ> metric;

    private ZSampleGrid<TZ> sampleGrid;

    private boolean closed = false;

    /**
     * @param metric TZ data "behavior" and "metric".
     */
    public AccumulativeGridSampler(ZSampleGrid<TZ> sampleGrid, AccumulativeMetric<TZ> metric) {
        this.metric = metric;
        this.sampleGrid = sampleGrid;
    }

    public final void addSamplingPoint(Coordinate C0, TZ z, double offRoadSpeed) {
        if (closed)
            throw new IllegalStateException("Can't add a sample after closing.");
        int[] xy = sampleGrid.getLowerLeftIndex(C0);
        int x = xy[0];
        int y = xy[1];
        @SuppressWarnings("unchecked")
        ZSamplePoint<TZ>[] ABCD = new ZSamplePoint[4];
        ABCD[0] = sampleGrid.getOrCreate(x, y);
        ABCD[1] = sampleGrid.getOrCreate(x + 1, y);
        ABCD[2] = sampleGrid.getOrCreate(x, y + 1);
        ABCD[3] = sampleGrid.getOrCreate(x + 1, y + 1);
        for (ZSamplePoint<TZ> P : ABCD) {
            Coordinate C = sampleGrid.getCoordinates(P);
            P.setZ(metric.cumulateSample(C0, C, z, P.getZ(), offRoadSpeed));
        }
    }

    /**
     * Surround all existing samples on the edge by a layer of closing samples.
     */
    public final void close() {
        if (closed)
            return;
        closed = true;
        List<ZSamplePoint<TZ>> processList = new ArrayList<ZSamplePoint<TZ>>(sampleGrid.size());
        for (ZSamplePoint<TZ> A : sampleGrid) {
            processList.add(A);
        }
        int round = 0;
        int n = 0;
        while (!processList.isEmpty()) {
            List<ZSamplePoint<TZ>> newProcessList = new ArrayList<ZSamplePoint<TZ>>(
                    processList.size());
            for (ZSamplePoint<TZ> A : processList) {
                if (A.right() == null) {
                    ZSamplePoint<TZ> B = closeSample(A.getX() + 1, A.getY());
                    if (B != null)
                        newProcessList.add(B);
                    n++;
                }
                if (A.left() == null) {
                    ZSamplePoint<TZ> B = closeSample(A.getX() - 1, A.getY());
                    if (B != null)
                        newProcessList.add(B);
                    n++;
                }
                if (A.up() == null) {
                    ZSamplePoint<TZ> B = closeSample(A.getX(), A.getY() + 1);
                    if (B != null)
                        newProcessList.add(B);
                    n++;
                }
                if (A.down() == null) {
                    ZSamplePoint<TZ> B = closeSample(A.getX(), A.getY() - 1);
                    if (B != null)
                        newProcessList.add(B);
                    n++;
                }
            }
            processList = newProcessList;
            LOG.debug("Round {} : next process list {}", round, processList.size());
            round++;
        }
        LOG.info("Added {} closing samples to get a total of {}.", n, sampleGrid.size());
    }

    private final ZSamplePoint<TZ> closeSample(int x, int y) {
        ZSamplePoint<TZ> A = sampleGrid.getOrCreate(x, y);
        boolean ok = metric.closeSample(A);
        if (ok) {
            return null;
        } else {
            return A;
        }
    }
}