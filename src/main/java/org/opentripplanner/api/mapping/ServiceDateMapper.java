package org.opentripplanner.api.mapping;

import org.opentripplanner.model.calendar.ServiceDate;

public class ServiceDateMapper {

    /**
     * Map a ServiceDate to the ISO 8601 standard YYYY-MM-DD.
     * If the given input date is {@code null} or <em>unbounded</em> then {@code null}
     * is returned.
     */
    public static String mapToApi(ServiceDate date) {
        return (date == null || date.isMinMax()) ? null : date.asISO8601();
    }
}
