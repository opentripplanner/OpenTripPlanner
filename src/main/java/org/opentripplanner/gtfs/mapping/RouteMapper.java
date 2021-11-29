package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Route;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Responsible for mapping GTFS Route into the OTP model. */
class RouteMapper {
    private final AgencyMapper agencyMapper;

    private final Map<org.onebusaway.gtfs.model.Route, Route> mappedRoutes = new HashMap<>();

    RouteMapper(AgencyMapper agencyMapper) {
        this.agencyMapper = agencyMapper;
    }

    Collection<Route> map(Collection<org.onebusaway.gtfs.model.Route> agencies) {
        return MapUtils.mapToList(agencies, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    Route map(org.onebusaway.gtfs.model.Route orginal) {
        return orginal == null ? null : mappedRoutes.computeIfAbsent(orginal, this::doMap);
    }

    private Route doMap(org.onebusaway.gtfs.model.Route rhs) {
        Route lhs = new Route(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));

        lhs.setAgency(agencyMapper.map(rhs.getAgency()));
        lhs.setShortName(rhs.getShortName());
        lhs.setLongName(rhs.getLongName());
        lhs.setType(rhs.getType());
        lhs.setMode(TransitModeMapper.mapMode(rhs.getType()));
        lhs.setDesc(rhs.getDesc());
        lhs.setUrl(rhs.getUrl());
        lhs.setColor(rhs.getColor());
        lhs.setTextColor(rhs.getTextColor());
        lhs.setBikesAllowed(BikeAccessMapper.mapForRoute(rhs));
        lhs.setSortOrder(rhs.getSortOrder());
        lhs.setBrandingUrl(rhs.getBrandingUrl());

        return lhs;
    }
}
