package org.opentripplanner.api.model;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.api.adapters.AgencyAndIdAdapter;
import org.opentripplanner.model.FeedScopedId;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Collection;
import java.util.List;

public class ApiStopShort {

    /**
     * Creates a short version of the ApiStop Model
     * @param stop
     */
    public ApiStopShort (ApiStop stop) {
        id = stop.id;
        lat = stop.stopLat;
        lon = stop.stopLon;
        code = stop.stopCode;
        name = stop.stopName;
        url = stop.stopUrl;
        cluster = stop.parentStation;
    }

    /** Distance to the stop when it is returned from a location-based query. */
    @JsonInclude(JsonInclude.Include.NON_NULL) public Integer dist;

    /** @param distance in integral meters, to avoid serializing a bunch of decimal places. */
    public ApiStopShort(ApiStop stop, int distance) {
        this(stop);
        this.dist = distance;
    }

    public static List<ApiStopShort> list (Collection<ApiStop> in) {
        List<ApiStopShort> out = Lists.newArrayList();
        for (ApiStop stop : in) out.add(new ApiStopShort(stop));
        return out;
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    public FeedScopedId id;

    @XmlAttribute
    @JsonSerialize
    public String name;

    @XmlAttribute
    @JsonSerialize
    public double lat;

    @XmlAttribute
    @JsonSerialize
    public double lon;

    @XmlAttribute
    @JsonSerialize
    public String code;

    @XmlAttribute
    @JsonSerialize
    public String url;

    @XmlAttribute
    @JsonSerialize
    public String cluster;
}
