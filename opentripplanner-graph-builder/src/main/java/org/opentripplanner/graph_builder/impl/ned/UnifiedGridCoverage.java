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

package org.opentripplanner.graph_builder.impl.ned;

import java.util.ArrayList;

import org.geotools.coverage.AbstractCoverage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.coverage.SampleDimension;
import org.opengis.geometry.DirectPosition;


public class UnifiedGridCoverage extends AbstractCoverage {

    private static final long serialVersionUID = -7798801307087575896L;

    // Used in the fix for #163. See comments below.
    private static final double OFFSET_HACK = 0.0001;
    
    private ArrayList<Coverage> regions;

    protected UnifiedGridCoverage(CharSequence name, Coverage coverage) {
        super(name, coverage);
        regions = new ArrayList<Coverage>();
        regions.add(coverage);
    }

    @Override
    public Object evaluate(DirectPosition point) throws PointOutsideCoverageException,
            CannotEvaluateException {
        /* we're don't care about this */
        return null;
    }

    /*
     * This is a big hack to get around the issues faced in #163:
     * http://opentripplanner.org/ticket/163
     * 
     * See http://osgeo-org.1803224.n2.nabble.com/Reading-tiff-files-td4970295.html#a4970295 and the
     * previous ticket for details on the issues here.
     * 
     * The workaround until the bug is fixed in GeoTools or another solution is found is: 
     *  * Try to evaluate the point
     *  * If the point is found, return the value
     *  * Otherwise, offset the point slightly and try again
     *  * Return the result
     *  
     *  Dumb, but it appears to work.
     */
    public double[] evaluate_orig(DirectPosition point, double[] values)
            throws PointOutsideCoverageException, CannotEvaluateException {
        for (Coverage region : regions) {
            double[] result;
            try {
                result = region.evaluate(point, values);
            } catch (PointOutsideCoverageException e) {
                continue;
            }
            return result;
        }
        /* not found */
        return null;
    }
    
    public double[] evaluate(DirectPosition point, double[] values)
            throws PointOutsideCoverageException, CannotEvaluateException {

        double[] result = evaluate_orig(point, values);
        if (result == null) {
            // If at first you don't succeed...
            result = evaluate_orig(new DirectPosition2D(
                    point.getCoordinate()[0] + OFFSET_HACK, 
                    point.getCoordinate()[1] + OFFSET_HACK), values);
        }
        return result;
    }
    
    @Override
    public int getNumSampleDimensions() {
        return regions.get(0).getNumSampleDimensions();
    }

    @Override
    public SampleDimension getSampleDimension(int index) throws IndexOutOfBoundsException {
        return regions.get(0).getSampleDimension(index);
    }

    public void add(GridCoverage2D regionCoverage) {
        regions.add(regionCoverage);
    }

}
