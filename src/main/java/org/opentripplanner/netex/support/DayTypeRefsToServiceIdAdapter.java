package org.opentripplanner.netex.support;

import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import java.util.HashSet;
import java.util.Set;

import static org.opentripplanner.netex.support.DayTypeRefToServiceIdMapper.generateServiceId;

/**
 * NeTEx do not have the GTFS concept of a ServiceId. In NeTEx a ServiceJourney is connected to a
 * ServiceCalendar using a set of {@link org.rutebanken.netex.model.DayTypeRefs_RelStructure}.
 * <p>
 * To create ServiceIds and avoid duplication we store all DayTypeRefs in a set using this class,
 * not the original. The reason is that the original DayTypeRefs do not implement hashCode()
 * and equals(), so we can not store it in a set.
 * <p>
 * This class is responsible for generating ServiceIds, any set of DayTypeRefs with identical refs
 * (order my differ) should result in the same unique ServiceId.
 * <p>
 * NeTEx implementation note: The DayTypeRefStructure version info is ignored.
 */
public final class DayTypeRefsToServiceIdAdapter {

    private final Set<String> dayTypeRefs;
    private final String serviceId;

    /**
     * Create an adapter base on the given {@code ref}.
     *
     * @return the new adapter or {@code null} if the input set is empty.
     */
    @Nullable
    public static DayTypeRefsToServiceIdAdapter create(DayTypeRefs_RelStructure refs) {
        Set<String> dayTypeRefs = collectDayTypeRefs(refs);
        return dayTypeRefs.isEmpty() ?  null : new DayTypeRefsToServiceIdAdapter(dayTypeRefs);
    }

    /**
     * Create a serviceId for the given dayTypes ids.
     *
     * @return the new ServiceId or {@code null} if the input set is empty.
     */
    @Nullable
    public static String createServiceId(DayTypeRefs_RelStructure refs) {
        Set<String> dayTypeRefs = collectDayTypeRefs(refs);
        return dayTypeRefs.isEmpty() ? null : generateServiceId(dayTypeRefs);
    }

    private static Set<String> collectDayTypeRefs(DayTypeRefs_RelStructure refs) {
        if(refs == null) { return Set.of(); }
        Set<String> dayTypeRefs = new HashSet<>();
        for (JAXBElement<? extends DayTypeRefStructure> e : refs.getDayTypeRef()) {
            // Keep the ref, ignore version info. Handling the version info is part of the
            // Nordic NeTEx profile, so this should probably be fixed. On the other hand
            // there is not problems reported on this.
            dayTypeRefs.add(e.getValue().getRef());
        }
        return dayTypeRefs;
    }

    public DayTypeRefsToServiceIdAdapter(Set<String> dayTypeRefs) {
        this.dayTypeRefs = dayTypeRefs;
        this.serviceId = generateServiceId(dayTypeRefs);
    }

    public Set<String> getDayTypeRefs() {
        return dayTypeRefs;
    }

    public String getServiceId() {
        return serviceId;
    }

    @Override
    public int hashCode() {
        return dayTypeRefs.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof DayTypeRefsToServiceIdAdapter)) return false;
        return dayTypeRefs.equals(((DayTypeRefsToServiceIdAdapter)obj).dayTypeRefs);
    }
}
