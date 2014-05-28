package org.opentripplanner.analyst;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.w3c.dom.Attr;

import javax.management.AttributeNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;

/**
 * A TimeSurface is evaluated at all the points in a PointSet to yield an Indicator.
 *
 * These are represented as a constrained format of GeoJSON.
 * They provide cumulative distributions for access to the opportunities in the PointSet
 * with respect to travel time from a particular origin.
 *
 * Indicators these are one of the three main web analyst resources:
 * Pointsets
 * Indicators
 * TimeSurfaces
 *
 * note: A TimeSurface is a lot like a Coverage. A Pointset is a lot like a FeatureCollection.
 *
 * An Indicator is structured exactly like a pointset.
 * In fact, it could be a subclass of Pointset with an extra field in each Attribute.
 *
 * Is it a one-to-many indicator, or many to many? Attributes.quantiles is an array, so
 * it's many-to-many.
 */
public class Indicator extends PointSet {

    PointSet origins; // the origins, which correspond one-to-one with the Quantiles array
    PointSet targets; // the targets that were checked for reachability in this indicator
    TimeSurface surface; // actually there is one per origin, not a single one!

    public Indicator (PointSet targets, TimeSurface surface) {
        this.targets = targets;
        this.surface = surface;
        this.nFeatures = 1;
        this.lons = new double[1];
        this.lats = new double[1];
        // Perform a deep copy of everything but the actual magnitudes for the attributes
        for (Category cat : targets.categories) {
            this.categories.add(new Category(cat));
        }
        // Evaluate the surface at all points in the sampleset
        float[] times = targets.samples.eval(surface);
        Quantiles quant = new Quantiles(times, 10);
        for (Category cat : categories) {
            for (Attribute attr : cat.attributes) {
                attr.quantiles = new Quantiles[1];
                attr.quantiles[0] = quant;
            }
        }
    }

    /**
     * Each origin will yield CSV with columns category,min,q25,q50,q75,max
     * Another column for the origin ID would allow this to extend to many-to-many.
     */
    void toCsv() {

    }


}
