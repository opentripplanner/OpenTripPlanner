package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;

import javax.xml.bind.JAXBElement;

public class ServiceIdMapper {

    public static String mapToServiceId(DayTypeRefs_RelStructure dayTypes) {
        StringBuilder serviceId = new StringBuilder();
        boolean first = true;
        for (JAXBElement dt : dayTypes.getDayTypeRef()) {
            if (!first) {
                serviceId.append("+");
            }
            first = false;
            if (dt.getValue() instanceof DayTypeRefStructure) {
                DayTypeRefStructure dayType = (DayTypeRefStructure) dt.getValue();
                serviceId.append(dayType.getRef());
            }
        }
        return serviceId.toString();
    }
}
