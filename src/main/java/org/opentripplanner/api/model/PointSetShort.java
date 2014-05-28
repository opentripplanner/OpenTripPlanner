package org.opentripplanner.api.model;

import com.google.common.collect.Lists;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.analyst.TimeSurface;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import org.opentripplanner.analyst.TimeSurface;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API data model object briefly describing a PointSet.
 */
public class PointSetShort {

    public String id;
    public int n;

    public PointSetShort(String id, PointSet pointSet) {
        this.id = id;
        this.n = pointSet.nFeatures;
    }

    public static List<PointSetShort> list (Map<String, PointSet> in) {
        List<PointSetShort> out = Lists.newArrayList();
        for (String id : in.keySet()) {
            out.add(new PointSetShort(id, in.get(id)));
        }
        return out;
    }

}
