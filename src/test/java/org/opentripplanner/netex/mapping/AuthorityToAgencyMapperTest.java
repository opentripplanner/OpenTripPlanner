package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.netex.mapping.MappingSupport.ID_FACTORY;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.organization.Agency;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.ContactStructure;
import org.rutebanken.netex.model.MultilingualString;

public class AuthorityToAgencyMapperTest {

  private static final String ID = "ID";
  private static final String NAME = "Olsen";
  private static final String SHORT_NAME = "Short";
  private static final String URL = "http://olsen.no/help";
  private static final String PHONE = "+47 88882222";
  private static final String TIME_ZONE = "CET";
  private static final String N_A = "N/A";

  private final AuthorityToAgencyMapper mapper = new AuthorityToAgencyMapper(ID_FACTORY, TIME_ZONE);

  @Test
  public void mapAgency() {
    // Given
    Authority authority = authority(ID, NAME, SHORT_NAME, URL, PHONE);

    // When mapped
    Agency a = mapper.mapAuthorityToAgency(authority);

    // Then expect
    assertEquals(ID, a.getId().getId());
    assertEquals(NAME, a.getName());
    assertEquals(TIME_ZONE, a.getTimezone().getId());
    assertEquals(URL, a.getUrl());
    assertEquals(PHONE, a.getPhone());
  }

  @Test
  public void mapAgencyWithoutOptionalElements() {
    // Given
    Authority authority = authority(ID, NAME, null, null, null);

    // When mapped
    Agency a = mapper.mapAuthorityToAgency(authority);

    // Then expect
    assertNull(a.getUrl());
    assertNull(a.getPhone());
  }

  @Test
  public void mapAgencyWithShortName() {
    // Given
    Authority authority = authority(ID, null, SHORT_NAME, null, null);

    // When mapped
    Agency a = mapper.mapAuthorityToAgency(authority);

    // Then expect
    assertEquals(SHORT_NAME, a.getName());
  }

  @Test
  public void getDefaultAgency() {
    // When mapped
    Agency a = mapper.createDummyAgency();

    // Then expect
    assertEquals("Dummy-" + a.getTimezone().getId(), a.getId().getId());
    assertEquals(N_A, a.getName());
    assertEquals(TIME_ZONE, a.getTimezone().getId());
    assertEquals(N_A, a.getUrl());
    assertEquals(N_A, a.getPhone());
  }

  @SuppressWarnings("SameParameterValue")
  private static Authority authority(
    String id,
    String name,
    String shortName,
    String url,
    String phone
  ) {
    return new Authority()
      .withId(id)
      .withShortName(new MultilingualString().withValue(shortName))
      .withName(new MultilingualString().withValue(name))
      .withContactDetails(new ContactStructure().withUrl(url).withPhone(phone));
  }
}
