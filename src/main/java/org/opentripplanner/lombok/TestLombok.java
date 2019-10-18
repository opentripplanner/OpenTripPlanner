package org.opentripplanner.lombok;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;

public class TestLombok {

    public static void test () {
        Route route = Route.builder()
                .id(FeedScopedId.builder().agencyId("AGENCY1").id("ROUTE1").build())
                .bikesAllowed(1)
                .build();
    }

}
