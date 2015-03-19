package org.opentripplanner.profile;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.opentripplanner.routing.core.TraverseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

// Jackson will serialize fields with getters, or @JsonProperty annotations.
public class ProfileResponse {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileResponse.class);

    public Set<Option> options = Sets.newHashSet();

    /**
     * The constructed response will include all the options that do not use transit,
     * as well as the top N options that do use transit for each access mode.
     *
     * @param allOptions a collection of Options with a mix of all access and egress modes, using transit or not.
     * @param orderBy specifies how the top N transit options will be chosen.
     * @param limit the maximum number of transit options to include in the response per access mode.
     *              zero or negative means no limit.
     */
    public ProfileResponse (Collection<Option> allOptions, Option.SortOrder orderBy, int limit) {
        List<Option> transitOptions = Lists.newArrayList();
        // Always return all non-transit options
        for (Option option : allOptions) {
            if (option.transit == null || option.transit.isEmpty()) options.add(option);
            else transitOptions.add(option);
        }
        // Order all transit options by the specified method
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
        // Group options by access mode, retaining ordering.
        // ListMultimap can hold duplicate key-value pairs and maintains the insertion ordering of values for a given key.
        // TODO update this to also use the egress mode in the key, and to consider the qualifiers on the modes
        ListMultimap<TraverseMode, Option> transitOptionsByAccessMode = ArrayListMultimap.create();
        for (Option option : transitOptions) {
            for (StreetSegment segment : option.access) {
                transitOptionsByAccessMode.put(segment.mode.mode, option);
            }
        }
        // Retain the top N transit options for each access mode. Duplicates may be present, but options is a Set.
        for (Collection<Option> singleModeOptions : transitOptionsByAccessMode.asMap().values()) {
            int n = 0;
            for (Option option : singleModeOptions) {
                options.add(option);
                if (limit > 0 && ++n >= limit) break;
            }
        }
        for (Option option : this.options) {
            LOG.info("{} {}", option.stats, option.summary);
        }
    }
    
}
