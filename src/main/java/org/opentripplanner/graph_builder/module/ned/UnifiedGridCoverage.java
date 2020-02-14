package org.opentripplanner.graph_builder.module.ned;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import org.geotools.coverage.AbstractCoverage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
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
    private final SpatialIndex regionsIndex;
    private final SpatialIndex datumsIndex;

    private List<GeneralEnvelope> regionEnvelopes;

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
        regionEnvelopes = new ArrayList<>();
        regionEnvelopes.add((GeneralEnvelope) coverage.getEnvelope());
        this.datums = datums;
        regionsIndex = new STRtree();
        datumsIndex = new STRtree();
        regionsIndex.insert(new ReferencedEnvelope(coverage.getEnvelope()), coverage);
        for (VerticalDatum datum : datums) {
            datumsIndex.insert(new Envelope(datum.lowerLeftLongitude, datum.lowerLeftLongitude + datum.deltaLongitude, datum.lowerLeftLatitude, datum.lowerLeftLatitude + datum.deltaLatitude), datum);
        }
    }

    @Override
    public Object evaluate(DirectPosition point) throws CannotEvaluateException {
        /* we don't use this function, we use evaluate(DirectPosition point, double[] values) */
        return null;
    }

    public double[] evaluate(DirectPosition point, double[] values) throws CannotEvaluateException {
        double x = point.getOrdinate(0);
        double y = point.getOrdinate(1);
        Coordinate pointCoordinate = new Coordinate(x, y);
        Envelope envelope = new Envelope(pointCoordinate);
        List<Coverage> coverageCandidates = regionsIndex.query(envelope);
        if (coverageCandidates.size() > 0) {
            // Found a match for coverage.
            Coverage region = coverageCandidates.get(0);
            List<VerticalDatum> datumCandidates = datumsIndex.query(envelope);
            if (datumCandidates.size() > 0) {
                // Found datum match.
                VerticalDatum datum = datumCandidates.get(0);
                double[] result;
                try {
                    result = region.evaluate(point, values);
                    result[0] += datum.interpolatedHeight(x, y);
                    return result;
                } catch (PointOutsideCoverageException e) {
                    /* not found */
                    log.warn("Point not found: " + point);
                    return null;
                }
            } else {
                //if we get here, all vdatums failed.
                log.error("Failed to convert elevation at " + y + ", " + x + " from NAVD88 to NAD83");
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
        regionsIndex.insert(new ReferencedEnvelope(regionCoverage.getEnvelope()), regionCoverage);
        regions.add(regionCoverage);
    }

}
