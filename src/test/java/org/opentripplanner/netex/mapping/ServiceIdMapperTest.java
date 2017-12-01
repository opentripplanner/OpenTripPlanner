package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import static org.junit.Assert.*;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (01.12.2017)
 */
public class ServiceIdMapperTest {
    @Test
    public void mapToServiceId() throws Exception {
        Class<DayTypeRefStructure> type = DayTypeRefStructure.class;
        DayTypeRefs_RelStructure value = new DayTypeRefs_RelStructure();
        createDTRS("REF");

        value.withDayTypeRef(wrap(createDTRS("A"), type), wrap(createDTRS("B"), type));

        assertEquals("A+B", ServiceIdMapper.mapToServiceId(value));

    }

    private DayTypeRefStructure createDTRS(String ref) {
        DayTypeRefStructure newObject = new DayTypeRefStructure();
        newObject.withRef(ref);
        return newObject;
    }

    private static <T> JAXBElement<T> wrap(T value, Class<T> clazz) {
        return new JAXBElement<>(new QName("x"), clazz, value);

    }
}