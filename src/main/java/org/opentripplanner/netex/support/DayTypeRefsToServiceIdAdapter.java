package org.opentripplanner.netex.support;

import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.HashSet;
import java.util.Set;

import static org.opentripplanner.netex.mapping.DayTypeRefToServiceIdMapper.generateServiceId;

/**
 * NeTEx do not have the GTFS concept of a ServiceId. In NeTEx a ServiceJourney is connected to a
 * ServiceCalendar using a set of {@link org.rutebanken.netex.model.DayTypeRefs_RelStructure}.
 * <p/>
 * To create ServiceIds and avoid duplication we store all DayTypeRefs in a set using this class,
 * not the original. The reason is that the original DayTypeRefs do not implement hashCode()
 * and equals(), so we can not store it in a set.
 * <p/>
 * This class is responsible for generating ServiceIds, any set of DayTypeRefs with identical refs
 * (order my differ) should result in the same unique ServiceId.
 * <p/>
 * NeTEx implementation note: The DayTypeRefStructure version info is ignored.
 */
public final class DayTypeRefsToServiceIdAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(DayTypeRefsToServiceIdAdapter.class);


    private final Set<String> dayTypeRefs = new HashSet<>();
    private String serviceId = null;

    /**
     * Create an adapter base on the given {@code ref}.
     */
    public DayTypeRefsToServiceIdAdapter(DayTypeRefs_RelStructure refs) {
        for (JAXBElement<? extends DayTypeRefStructure> e : refs.getDayTypeRef()) {
            // Keep the ref, ignore version info. Handling the version info is part of the
            // Norwegian NeTEx profile, so this should probably be fixed. On the other hand
            // there is not problems reported on this.
            dayTypeRefs.add(e.getValue().getRef());
        }
        if(dayTypeRefs.isEmpty()) {
            LOG.warn("DayTypeRefs is empty - not expected: " + refs);
        }
        this.serviceId = generateServiceId(dayTypeRefs);
    }

    public Set<String> getDayTypeRefs() {
        return dayTypeRefs;
    }

    public String getServiceId() {
        return serviceId;
    }

    @Override public int hashCode() {
        return dayTypeRefs.hashCode();
    }

    @Override public boolean equals(Object obj) {
        if(!(obj instanceof DayTypeRefsToServiceIdAdapter)) return false;
        return dayTypeRefs.equals(((DayTypeRefsToServiceIdAdapter)obj).dayTypeRefs);
    }
}
