package org.opentripplanner.api.model;

import com.beust.jcommander.internal.Lists;

import java.util.Collection;

public class ApiPatternDetail extends ApiPatternShort {

    /* Maybe these should just be lists of IDs only, since there are stops and trips subendpoints. */
    public Collection<ApiStopShort> stops = Lists.newArrayList();
    public Collection<ApiTripShort> trips = Lists.newArrayList();
}
