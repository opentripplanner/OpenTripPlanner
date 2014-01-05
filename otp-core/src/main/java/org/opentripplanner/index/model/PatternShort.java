package org.opentripplanner.index.model;

import java.util.Collection;
import java.util.List;

import org.opentripplanner.routing.edgetype.TableTripPattern;

import com.beust.jcommander.internal.Lists;

public class PatternShort {

    public String id;
    public String desc;
    
    public PatternShort (TableTripPattern pattern) {
        id = pattern.getCode();
        desc = pattern.getName();
    }
    
    public static List<PatternShort> list (Collection<TableTripPattern> in) {
        List<PatternShort> out = Lists.newArrayList();
        for (TableTripPattern pattern : in) out.add(new PatternShort(pattern));
        return out;
    }    
    
}
