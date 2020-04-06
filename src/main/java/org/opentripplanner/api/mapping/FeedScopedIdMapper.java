package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiFeedScopedId;
import org.opentripplanner.model.FeedScopedId;

public class FeedScopedIdMapper {

    public static FeedScopedId mapToDomain(ApiFeedScopedId arg) {
        if (arg == null) {
            return null;
        }
        return new FeedScopedId(arg.agency, arg.id);
    }

    public static ApiFeedScopedId mapToApi(FeedScopedId arg) {
        if (arg == null) {
            return null;
        }
        return new ApiFeedScopedId(arg.getFeedId(), arg.getId());
    }

}
