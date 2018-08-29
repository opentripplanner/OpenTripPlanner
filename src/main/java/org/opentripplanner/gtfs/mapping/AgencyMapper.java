package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Responsible for mapping GTFS Agency into the OTP model. */
class AgencyMapper {

//    // Work around the fact that OBA agencies have no scope - in OBA they are the scope for all other identifiers.
//    private final String feedId;

    // Note we are keying on only the OBA agency which has no feed scope. This works because we load one feed at a time
    // i.e. we make a new AgencyMapper as we load each new feed.
    private final Map<org.onebusaway.gtfs.model.Agency, Agency> mappedAgencies = new HashMap<>();

//    /**
//     * @param feedId all agencies mapped by this mapper will have the provided feed id.
//     */
//    public AgencyMapper(String feedId) {
//        this.feedId = feedId;
//    }

    Collection<Agency> map(Collection<org.onebusaway.gtfs.model.Agency> agencies) {
        return MapUtils.mapToList(agencies, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    Agency map(org.onebusaway.gtfs.model.Agency orginal) {
        return orginal == null ? null : mappedAgencies.computeIfAbsent(orginal, this::doMap);
    }

    private Agency doMap(org.onebusaway.gtfs.model.Agency rhs) {
        Agency lhs = new Agency();
        lhs.setId(rhs.getId()); // rhs.getId() == null ? null : new FeedScopedId(feedId, rhs.getId()));
        lhs.setName(rhs.getName());
        lhs.setUrl(rhs.getUrl());
        lhs.setTimezone(rhs.getTimezone());
        lhs.setLang(rhs.getLang());
        lhs.setPhone(rhs.getPhone());
        lhs.setFareUrl(rhs.getFareUrl());
        lhs.setBrandingUrl(rhs.getBrandingUrl());
        return lhs;
    }
}
