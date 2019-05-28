package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (01.12.2017)
 */
public class ServiceIdMapperTest {

    @SuppressWarnings("unchecked")
    @Test
    public void mapToServiceId() {
        Class<DayTypeRefStructure> type = DayTypeRefStructure.class;
        DayTypeRefs_RelStructure value = new DayTypeRefs_RelStructure();
        createDayTypeRefStructure("REF");

        value.withDayTypeRef(
                wrap(createDayTypeRefStructure("A"), type),
                wrap(createDayTypeRefStructure("B"), type)
        );

        assertEquals("A+B", ServiceIdMapper.mapToServiceId(value));

    }

    private DayTypeRefStructure createDayTypeRefStructure(String ref) {
        DayTypeRefStructure newObject = new DayTypeRefStructure();
        newObject.withRef(ref);
        return newObject;
    }

    private static <T> JAXBElement<T> wrap(T value, Class<T> clazz) {
        return new JAXBElement<>(new QName("x"), clazz, value);

    }
}