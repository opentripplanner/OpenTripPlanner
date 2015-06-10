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

package org.opentripplanner.graph_builder.module.ned;

import org.geotools.coverage.AbstractCoverage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.GeneralEnvelope;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.coverage.SampleDimension;
import org.opengis.geometry.DirectPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Stitches together multiple elevation maps into a single elevation map,
 * hackily.  This is horrible, but the geotools way of doing things is
 * too slow.   
 * @author novalis
 *
 */
public class UnifiedGridCoverage extends AbstractCoverage {

    private static final long serialVersionUID = -7798801307087575896L;

    private static Logger log = LoggerFactory.getLogger(UnifiedGridCoverage.class);
    
    private ArrayList<Coverage> regions;

    private List<VerticalDatum> datums;

    /**
     * It would be nice if we could construct this unified coverage with zero sub-coverages and add all sub-coverages
     * in the same way. However, the superclass constructor (AbstractCoverage) needs a coverage to copy properties from.
     * So the first sub-coverage needs to be passed in at construction time.
     */
    protected UnifiedGridCoverage(CharSequence name, Coverage coverage, List<VerticalDatum> datums) {
        super(name, coverage);
        regions = new ArrayList<Coverage>();
        regions.add(coverage);
        this.datums = datums;
    }

    @Override
    public Object evaluate(DirectPosition point) throws PointOutsideCoverageException, CannotEvaluateException {
        /* we don't use this function, we use evaluate(DirectPosition point, double[] values) */
        return null;
    }

    public double[] evaluate(DirectPosition point, double[] values)
            throws PointOutsideCoverageException, CannotEvaluateException {

        for (Coverage region : regions) {
            // GeneralEnvelope has a contains method, OpenGIS Envelope does not
            GeneralEnvelope env = ((GeneralEnvelope)region.getEnvelope());
            // Check envelope to avoid incurring exception construction overhead (PointOutsideCoverageException),
            // especially important when there are many regions.
            if (env.contains(point)) {
                double[] result;
                double x = point.getOrdinate(0);
                double y = point.getOrdinate(1);
                try {
                    result = region.evaluate(point, values);
                    // TODO It might be faster to put all the datums and Coverage regions into a spatial index instead of iterating.
                    for (VerticalDatum datum : datums) {
                        if (datum.covers(x, y)) {
                            result[0] += datum.interpolatedHeight(x, y);
                            return result;
                        }
                    }
                    //if we get here, all vdatums failed.
                    log.error("Failed to convert elevation at " + y + ", " + x + " from NAVD88 to NAD83");
                } catch (PointOutsideCoverageException e) {
                    continue;
                }
                return result;
            }
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
