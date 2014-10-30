package org.opentripplanner.gtfs.validator;

import org.opentripplanner.gtfs.GTFSFeed;

/**
 *
 */
public class DefaultValidator extends GTFSValidator {

    @Override
    public boolean validate(GTFSFeed feed, boolean repair) {

        return false;
    }

}
