package org.opentripplanner.analyst;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Just like IndicatorLite, plus this carries around the TimeSurface
 */

public class Indicator extends IndicatorLite {

	protected PointSet origins; // the origins, which correspond one-to-one with the Quantiles array
    protected TimeSurface surface; // actually there is one per origin, not a single one!
     
    public Indicator (SampleSet samples, TimeSurface surface, boolean retainTimes) {
        super(samples, surface, retainTimes); // for now we only do one-to-many
        this.surface = surface;
    }
		
}
