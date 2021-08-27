package org.opentripplanner.index.model;

import java.util.Collection;
import java.util.List;

import org.opentripplanner.routing.edgetype.TripPattern;

import com.beust.jcommander.internal.Lists;
import org.opentripplanner.util.model.EncodedPolylineBean;

public class PatternShort {

    public String id;
    public String desc;
    public EncodedPolylineBean geometry;

    /**
     * Constructor for maintaining backwards compatibility
     */
    public PatternShort (TripPattern pattern) {
        id = pattern.code;
        desc = pattern.name;
        geometry = null;
    }

    /**
     * Constructor for when geometry is passed
     */
    public PatternShort (TripPattern pattern, EncodedPolylineBean geometries) {
        id = pattern.code;
        desc = pattern.name;
        geometry = geometries;
    }

    /**
     * If geometry is passed, pattern input must also be List to allow indexing.
     */
    public static List<PatternShort> list (List<TripPattern> in, List<EncodedPolylineBean> geometries) {
        List<PatternShort> out = Lists.newArrayList();
        for (int i = 0; i < in.size(); i++) out.add(new PatternShort(in.get(i), geometries.get(i)));
        return out;
    }

    /**
     * Maintain backwards compatibility for when geometry is not supplied
     */
    public static List<PatternShort> list (Collection<TripPattern> in) {
        List<PatternShort> out = Lists.newArrayList();
        for (TripPattern pattern : in) out.add(new PatternShort(pattern));
        return out;
    }

}
