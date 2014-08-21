package org.opentripplanner.profile;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Jackson will serialize fields with getters, or @JsonProperty annotations.
public class ProfileResponse {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileResponse.class);

    List<Option> options = Lists.newArrayList();

    /**
     * The constructed response will include all the options that do not use transit,
     * as well as the top N options that do use transit.
     *
     * @param orderBy specifies how the top N transit options will be chosen
     * @param limit the maximum number of transit options to include in the response.
     */
    public ProfileResponse (Collection<Option> options, Option.SortOrder orderBy, int limit) {
        List<Option> transitOptions = Lists.newArrayList();
        // Always include non-transit options
        for (Option option : options) {
            if (option.transit == null || option.transit.isEmpty()) this.options.add(option);
            else transitOptions.add(option);
        }
        // Then choose the top N transit options
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
        Collections.sort(transitOptions, c);
        if (limit > 0 && limit <= transitOptions.size()) {
            transitOptions = transitOptions.subList(0, limit);
        }
        this.options.addAll(transitOptions);
        for (Option option : this.options) {
            LOG.info("{} {}", option.stats, option.summary);
        }
    }
    
}
