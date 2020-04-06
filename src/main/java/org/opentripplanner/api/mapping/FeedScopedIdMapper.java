package org.opentripplanner.api.mapping;

import org.opentripplanner.model.FeedScopedId;

public class FeedScopedIdMapper {


    private static final String SEPARATOR = ":";

    public static FeedScopedId mapToDomain(String api) {
        if (api == null) { return null; }
        String[] parts = api.split(SEPARATOR, 2);
        return new FeedScopedId(parts[0], parts[1]);
    }

    public static String mapToApi(FeedScopedId arg) {
        if (arg == null) {
            return null;
        }
        return arg.getFeedId() + ":" + arg.getId();
    }
}
