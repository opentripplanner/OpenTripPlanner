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
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.coverage.SampleDimension;
import org.opengis.geometry.DirectPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UnifiedGridCoverage extends AbstractCoverage {

    private static final long serialVersionUID = -7798801307087575896L;

    private static Logger log = LoggerFactory.getLogger(UnifiedGridCoverage.class);
    
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

    public double[] evaluate(DirectPosition point, double[] values)
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
        log.warn("Point not found: " + point);
        
        return null;
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
