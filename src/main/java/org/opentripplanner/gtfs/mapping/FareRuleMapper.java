package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.FareRule;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Responsible for mapping GTFS FareRule into the OTP model. */
class FareRuleMapper {

    private final RouteMapper routeMapper;

    private final FareAttributeMapper fareAttributeMapper;

    private Map<org.onebusaway.gtfs.model.FareRule, FareRule> mappedFareRules = new HashMap<>();

    FareRuleMapper(RouteMapper routeMapper, FareAttributeMapper fareAttributeMapper) {
        this.routeMapper = routeMapper;
        this.fareAttributeMapper = fareAttributeMapper;
    }

    Collection<FareRule> map(Collection<org.onebusaway.gtfs.model.FareRule> allFareRules) {
        return MapUtils.mapToList(allFareRules, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    FareRule map(org.onebusaway.gtfs.model.FareRule orginal) {
        return orginal == null ? null : mappedFareRules.computeIfAbsent(orginal, this::doMap);
    }

    private FareRule doMap(org.onebusaway.gtfs.model.FareRule rhs) {
        FareRule lhs = new FareRule();

        lhs.setFare(fareAttributeMapper.map(rhs.getFare()));
        lhs.setRoute(routeMapper.map(rhs.getRoute()));
        lhs.setOriginId(rhs.getOriginId());
        lhs.setDestinationId(rhs.getDestinationId());
        lhs.setContainsId(rhs.getContainsId());

        return lhs;
    }
}
