package org.opentripplanner.netex.loader.mapping;

import org.junit.Test;
import org.opentripplanner.model.Agency;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.ContactStructure;
import org.rutebanken.netex.model.MultilingualString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opentripplanner.netex.loader.mapping.MappingSupport.ID_FACTORY;

public class AuthorityToAgencyMapperTest {
    private static final String ID = "ID";
    private static final String NAME = "Olsen";
    private static final String URL = "http://olsen.no/help";
    private static final String PHONE = "+47 88882222";
    private static final String TIME_ZONE = "CEST";
    private static final String N_A = "N/A";

    private AuthorityToAgencyMapper mapper = new AuthorityToAgencyMapper(ID_FACTORY, TIME_ZONE);

    @Test public void mapAgency() {
        // Given
        Authority authority = authority(ID, NAME, URL, PHONE);

        // When mapped
        Agency a = mapper.mapAuthorityToAgency(authority);

        // Then expect
        assertEquals(ID, a.getId().getId());
        assertEquals(NAME, a.getName());
        assertEquals(TIME_ZONE, a.getTimezone());
        assertEquals(URL, a.getUrl());
        assertEquals(PHONE, a.getPhone());
    }

    @Test public void mapAgencyWithoutOptionalElements() {
        // Given
        Authority authority = authority(ID, NAME, null, null);

        // When mapped
        Agency a = mapper.mapAuthorityToAgency(authority);

        // Then expect
        assertNull(a.getUrl());
        assertNull(a.getPhone());
    }

    @Test public void getDefaultAgency() {
        // When mapped
        Agency a = mapper.createDummyAgency();

        // Then expect
        assertEquals("Dummy-" + a.getTimezone(), a.getId().getId());
        assertEquals(N_A, a.getName());
        assertEquals(TIME_ZONE, a.getTimezone());
        assertEquals(N_A, a.getUrl());
        assertEquals(N_A, a.getPhone());
    }

    @SuppressWarnings("SameParameterValue")
    private static Authority authority(String id, String name, String url, String phone) {
        return new Authority()
                .withId(id)
                .withName(new MultilingualString().withValue(name))
                .withContactDetails(
                        new ContactStructure().withUrl(url).withPhone(phone)
                );
    }
}