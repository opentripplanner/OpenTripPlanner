package org.opentripplanner.profile;

import java.util.List;

import lombok.Getter;

import org.opentripplanner.profile.ProfileRouter.Ride;

import com.google.common.collect.Lists;

// Jackson will serialize fields with getters, or @JsonProperty annotations.
public class Response {
    
    @Getter
    List<Option> options = Lists.newArrayList();
    
    private void addOption (Ride ride) {
        options.add(new Option(ride));
    }
    
    public Response (Iterable<Ride> options) {
        for (Ride option : options) addOption(option);
    }
    
}
