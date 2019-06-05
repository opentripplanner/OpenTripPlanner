package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.rutebanken.netex.model.DayTypeRefStructure;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opentripplanner.netex.mapping.DayTypeRefToServiceIdMapper.generateServiceId;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (01.12.2017)
 */
public class DayTypeRefToServiceIdMapperTest {

    @Test
    public void mapToServiceId() {
        assertEquals("A", generateServiceId(Collections.singletonList("A")));
        assertEquals("A+B", generateServiceId(Arrays.asList("A", "B")));
        assertNull(generateServiceId(Collections.emptyList()));

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