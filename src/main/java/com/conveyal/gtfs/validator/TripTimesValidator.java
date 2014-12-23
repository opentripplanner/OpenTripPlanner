package com.conveyal.gtfs.validator;

import com.conveyal.gtfs.GTFSFeed;

public class TripTimesValidator extends GTFSValidator {

    @Override
    public boolean validate(GTFSFeed feed, boolean repair) {
        // When ordering trips by stop_sequence, stoptimes should be increasing, speeds within range
        // Trips should have at least one hop (at least two stops)
        // Speed should not be infinite (check distance, time separately)
        return false;
    }

}
