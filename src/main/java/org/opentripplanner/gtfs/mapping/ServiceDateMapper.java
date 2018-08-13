package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.calendar.ServiceDate;

class ServiceDateMapper {
    static ServiceDate mapServiceDate(org.onebusaway.gtfs.model.calendar.ServiceDate orginal) {
        return orginal == null ?
                null :
                new ServiceDate(orginal.getYear(), orginal.getMonth(), orginal.getDay());
    }
}
