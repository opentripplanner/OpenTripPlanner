package org.opentripplanner.profile;

import java.util.Collection;
import java.util.List;

import lombok.Getter;

import com.google.common.collect.Lists;

// Jackson will serialize fields with getters, or @JsonProperty annotations.
public class ProfileResponse {
    
    @Getter
    List<Option> options = Lists.newArrayList();
    
    public ProfileResponse (Collection<Option> options) {
        this.options.addAll(options);
    }
    
}
