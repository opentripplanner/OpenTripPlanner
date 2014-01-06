package org.opentripplanner.profile;

import java.util.Collection;
import java.util.List;

import lombok.Getter;

import com.google.common.collect.Lists;

// Jackson will serialize fields with getters, or @JsonProperty annotations.
public class Response {
    
    @Getter
    List<Option> options = Lists.newArrayList();
    
    public Response (Collection<Option> options) {
        this.options.addAll(options);
    }
    
}
