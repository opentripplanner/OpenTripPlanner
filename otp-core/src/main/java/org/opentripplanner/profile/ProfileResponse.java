package org.opentripplanner.profile;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import lombok.Getter;

import com.google.common.collect.Lists;

// Jackson will serialize fields with getters, or @JsonProperty annotations.
public class ProfileResponse {
    
    @Getter
    List<Option> options = Lists.newArrayList();
    
    public ProfileResponse (Collection<Option> options, Option.SortOrder orderBy, int limit) {
        this.options.addAll(options);
        Comparator<Option> c;
        switch (orderBy) {
            case MAX:
                c = new Option.MaxComparator();
                break;
            case AVG:
                c = new Option.AvgComparator();
                break;
            case MIN:
            default:
                c = new Option.MinComparator();
        }
        Collections.sort(this.options, c);
        if (limit > 0 && limit <= options.size()) {
            this.options = this.options.subList(0, limit);
        }
    }
    
}
