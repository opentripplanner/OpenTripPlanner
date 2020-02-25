package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiStop;
import org.opentripplanner.model.Stop;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps a Stop Model to an ApiStop Model and a list of Stops to a list of ApiStops
 */
public class StopMapper {

    public StopMapper( ) {

    }

    /**
     * Creates a single apiStop instance from a Stop model and returns it
     * @param domain Stop model
     * @return an ApiStop model
     */
    public static ApiStop mapStop(Stop domain) {
        ApiStop api = new ApiStop(domain);
        return api;
    }

    /**
     * Creates a list of ApiStops from a collection of Stop models and returns it
     * @param domain Collection of Stop models
     * @return a List of ApiStop models
     */
    public static List<ApiStop> mapStops(Collection<Stop> domain) {
        if(domain == null) { return null; }
        return domain.stream().map(StopMapper::mapStop).collect(Collectors.toList());
    }
}
