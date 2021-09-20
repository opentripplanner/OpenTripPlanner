package org.opentripplanner.index.model;

import com.beust.jcommander.internal.Lists;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a pattern but only showing a subset of the available information. Optionally includes
 * geometry of the pattern.
 */
public class PatternShort {

    public String id;
    public String desc;
    public EncodedPolylineBean geometry;

    /**
     * Constructor for when includeGeometry parameter is not passed
     */
    public PatternShort (TripPattern pattern) {
        this(pattern, false);
    }

    /**
     * Constructor for when geometry is passed
     */
    public PatternShort (TripPattern pattern, boolean includeGeometry) {
        id = pattern.code;
        desc = pattern.name;
        if (includeGeometry) {
            geometry = PolylineEncoder.createEncodings(pattern.geometry);
        }
    }

    public static List<PatternShort> list (Collection<TripPattern> in, boolean includeGeometry) {
        List<PatternShort> out = Lists.newArrayList();
        for (TripPattern pattern : in) out.add(new PatternShort(pattern, includeGeometry));
        return out;
    }

}
