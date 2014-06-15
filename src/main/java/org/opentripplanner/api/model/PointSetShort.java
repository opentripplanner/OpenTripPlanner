package org.opentripplanner.api.model;

import com.google.common.collect.Lists;
import org.opentripplanner.analyst.PointSet;

import java.util.List;
import java.util.Map;

/**
 * API data model object briefly describing a PointSet.
 */
public class PointSetShort {

    public String id;
    public Integer n;

    public PointSetShort(String id, PointSet pointSet) {
        this.id = id;
        if(pointSet != null)
        	this.n = pointSet.capacity;
    }

    public static List<PointSetShort> list (List<String> in) {
        List<PointSetShort> out = Lists.newArrayList();
        for (String id : in) {
            out.add(new PointSetShort(id, null));
        }
        return out;
    }
    
    public static List<PointSetShort> list (Map<String, PointSet> in) {
        List<PointSetShort> out = Lists.newArrayList();
        for (String id : in.keySet()) {
            out.add(new PointSetShort(id, in.get(id)));
        }
        return out;
    }

}
