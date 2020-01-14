package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.calendar.ServiceDateInterval;

/** Responsible for mapping GTFS ServiceDate into the OTP model. */
class ServiceDateMapper {
    /** Map from GTFS to OTP model, {@code null} safe.  */
    static ServiceDate mapServiceDate(org.onebusaway.gtfs.model.calendar.ServiceDate orginal) {
        return orginal == null ?
                null :
                new ServiceDate(orginal.getYear(), orginal.getMonth(), orginal.getDay());
    }

    static ServiceDateInterval mapServiceDateInterval(
            org.onebusaway.gtfs.model.calendar.ServiceDate start,
            org.onebusaway.gtfs.model.calendar.ServiceDate end
    ) {
        return new ServiceDateInterval(
                mapServiceDate(start),
                mapServiceDate(end)
        );
    }

}
