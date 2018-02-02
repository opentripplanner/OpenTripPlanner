package org.opentripplanner.index.model;

import java.util.Collection;
import java.util.List;

import org.opentripplanner.routing.edgetype.TripPattern;

import com.beust.jcommander.internal.Lists;

/**
 * Represents a TripPattern in the API response. TripPattern is a data structure created by OTP to represent
 * a particular stopping pattern of a route.
 */
public class PatternShort {

    /** ID of this TripPattern. This is unique to OTP and should not be displayed to customers. */
    public String id;

    /** Description of this TripPattern. This is unique to OTP and should not be displayed to customers. */
    public String desc;
    
    public PatternShort (TripPattern pattern) {
        id = pattern.code;
        desc = pattern.name;
    }
    
    public static List<PatternShort> list (Collection<TripPattern> in) {
        List<PatternShort> out = Lists.newArrayList();
        for (TripPattern pattern : in) out.add(new PatternShort(pattern));
        return out;
    }    
    
}
