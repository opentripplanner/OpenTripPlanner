package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.IgnoredAlert;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Responsible for mapping MBTA GTFS Ignored Alerts entities into the OTP model. */
class IgnoredAlertMapper {

    private final Map<org.onebusaway.gtfs.model.IgnoredAlert, IgnoredAlert> mappedAlerts = new HashMap<>();

    Collection<IgnoredAlert> map(Collection<org.onebusaway.gtfs.model.IgnoredAlert> alerts) {
        return MapUtils.mapToList(alerts, this::map);
    }

    /** Maps from GTFS to OTP model, {@code null} safe.  */
    IgnoredAlert map(org.onebusaway.gtfs.model.IgnoredAlert orginal) {
        return orginal == null ? null : mappedAlerts.computeIfAbsent(orginal, this::doMap);
    }

    private IgnoredAlert doMap(org.onebusaway.gtfs.model.IgnoredAlert rhs) {
        IgnoredAlert lhs = new IgnoredAlert();

        lhs.setId(rhs.getId());
        lhs.setDescription(rhs.getDescription());

        return lhs;
    }
}
