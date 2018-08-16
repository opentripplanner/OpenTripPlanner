package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.calendar.ServiceDate;

/** Responsible for mapping GTFS ServiceDate into the OTP model. */
class ServiceDateMapper {
    /** Map from GTFS to OTP model, {@code null} safe.  */
    static ServiceDate mapServiceDate(org.onebusaway.gtfs.model.calendar.ServiceDate orginal) {
        return orginal == null ?
                null :
                new ServiceDate(orginal.getYear(), orginal.getMonth(), orginal.getDay());
    }
}
