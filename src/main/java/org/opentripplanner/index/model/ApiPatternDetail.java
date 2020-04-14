package org.opentripplanner.index.model;

import com.beust.jcommander.internal.Lists;
import org.opentripplanner.api.model.ApiPatternShort;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

@XmlRootElement(name = "PatternDetail")
public class ApiPatternDetail extends ApiPatternShort {

    /* Maybe these should just be lists of IDs only, since there are stops and trips subendpoints. */
    public String routeId;
    public Collection<ApiStopShort> stops = Lists.newArrayList();
    public Collection<ApiTripShort> trips = Lists.newArrayList();
}
