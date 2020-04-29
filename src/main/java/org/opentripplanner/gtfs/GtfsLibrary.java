package org.opentripplanner.gtfs;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;


public class GtfsLibrary {

    private static final char ID_SEPARATOR = ':'; // note this is different than what OBA GTFS uses to match our 1.0 API

    /* Using in index since we can't modify OBA libs and the colon in the expected separator in the 1.0 API. */
    public static FeedScopedId convertIdFromString(String value) {
        int index = value.indexOf(ID_SEPARATOR);
        if (index == -1) {
            throw new IllegalArgumentException("invalid agency-and-id: " + value);
        } else {
            return new FeedScopedId(value.substring(0, index), value.substring(index + 1));
        }
    }

    public static String convertIdToString(FeedScopedId aid) {
        return aid.getFeedId() + ID_SEPARATOR + aid.getId();
    }

    /** @return the route's short name, or the long name if the short name is null. */
    public static String getRouteName(Route route) {
        if (route.getShortName() != null)
            return route.getShortName();
        return route.getLongName();
    }
}
