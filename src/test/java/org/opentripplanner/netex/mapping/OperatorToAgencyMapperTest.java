package org.opentripplanner.netex.mapping;


import org.junit.Test;
import org.rutebanken.netex.model.ContactStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Operator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OperatorToAgencyMapperTest {
    private static final String ID = "ID";
    private static final String NAME = "Olsen";
    private static final String URL = "http://olsen.no/help";
    private static final String PHONE = "+47 88882222";

    @Test public void mapOperatorWithEverything() {
        // Given
        Operator operator = new Operator()
                .withId(ID)
                .withName(new MultilingualString().withValue(NAME))
                .withContactDetails(
                    new ContactStructure().withUrl(URL).withPhone(PHONE)
                );

        // When mapped
        org.opentripplanner.model.Operator o;
        o = new OperatorToAgencyMapper(MappingSupport.ID_FACTORY).mapOperator(operator);

        // Then expect
        assertEquals(ID, o.getId().getId());
        assertEquals(NAME, o.getName());
        assertEquals(URL, o.getUrl());
        assertEquals(PHONE, o.getPhone());
    }

    @Test public void mapOperatorWithMinimumDataSet() {
        // Given
        Operator operator = new Operator().withId(ID).withName(new MultilingualString().withValue(NAME));

        // When mapped
        org.opentripplanner.model.Operator o;
        o = new OperatorToAgencyMapper(MappingSupport.ID_FACTORY).mapOperator(operator);

        // Then expect
        assertEquals(ID, o.getId().getId());
        assertEquals(NAME, o.getName());
        assertNull(o.getUrl());
        assertNull(o.getPhone());
    }
}
