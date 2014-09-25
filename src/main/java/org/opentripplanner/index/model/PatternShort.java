package org.opentripplanner.index.model;

import java.util.Collection;
import java.util.List;

import org.opentripplanner.routing.edgetype.TripPattern;

import com.beust.jcommander.internal.Lists;

public class PatternShort {

    public String id;
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
