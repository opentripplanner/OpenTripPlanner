package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;

import javax.xml.bind.JAXBElement;

// TODO OTP2 - this need documentation. Is is specified some where that '+' should
// TODO OTP2 - be used as a separator, or it is used to because it does not create
// TODO OTP2 - some sort of conflict. We have had some issues with the AgencyAndId
// TODO OTP2 - strategy in the past, so a better approach would perhaps be to avoid
// TODO OTP2 - any such conventions. It can be fixed by creating a ServiceId type
// TODO OTP2 - and have different subtypes that clearly reveal the intended
// TODO OTP2 - workaround.
// TODO OTP2 - Add Unit tests

/**
 * The concept ServiceId does not exist in NeTEx. To generate a Service Id a simple
 * strategy of concatenating the dates together is used.
 */
public class ServiceIdMapper {
    private static final char SEP = '+';



    public static String generateToServiceId(DayTypeRefs_RelStructure dayTypes) {
        StringBuilder serviceId = new StringBuilder();
        boolean first = true;

        for (JAXBElement<? extends DayTypeRefStructure> element : dayTypes.getDayTypeRef()) {
            if (!first) {
                serviceId.append(SEP);
            }
            first = false;
            serviceId.append(element.getValue().getRef());
        }
        return serviceId.toString();
    }

    /**
     * dayTypeIds
     * @param netexServiceId
     * @return
     */
    public static String[] parseServiceId(String netexServiceId) {
        return netexServiceId.split("\\" + SEP);
    }
}
