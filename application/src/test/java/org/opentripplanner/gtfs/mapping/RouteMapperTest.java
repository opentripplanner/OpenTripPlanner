package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.organization.Branding;

class RouteMapperTest {

  private static final String FEED_ID = "A";

  private static final Agency AGENCY = new GtfsTestData().agency;

  private static final AgencyAndId ROUTE_ID = new AgencyAndId(FEED_ID, "1");

  private static final String SHORT_NAME = "Short Name";

  private static final String NETWORK_ID = "network id";

  private static final String LONG_NAME = "Long Name";

  private static final String DESC = "Desc";

  private static final Integer ROUTE_TYPE = 2;

  private static final TransitMode TRANSIT_MODE = TransitMode.RAIL;

  private static final String URL = "www.url.me";

  private static final String COLOR = "green";

  private static final String TEXT_COLOR = "red";

  private static final int BIKES_ALLOWED = 1;

  private static final Integer SORT_ORDER = 1;

  private static final Route ROUTE = new Route();
  private static final IdFactory ID_FACTORY = new IdFactory(FEED_ID);
  private final RouteMapper subject = new RouteMapper(
    ID_FACTORY,
    new AgencyMapper(ID_FACTORY),
    DataImportIssueStore.NOOP,
    new TranslationHelper()
  );

  static {
    ROUTE.setId(ROUTE_ID);
    ROUTE.setAgency(AGENCY);
    ROUTE.setShortName(SHORT_NAME);
    ROUTE.setLongName(LONG_NAME);
    ROUTE.setDesc(DESC);
    ROUTE.setType(ROUTE_TYPE);
    ROUTE.setUrl(URL);
    ROUTE.setColor(COLOR);
    ROUTE.setTextColor(TEXT_COLOR);
    ROUTE.setBikesAllowed(BIKES_ALLOWED);
    ROUTE.setSortOrder(SORT_ORDER);
  }

  @Test
  void testMapCollection() throws Exception {
    assertNull(subject.map((Collection<Route>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(ROUTE)).size());
  }

  @Test
  void testMap() throws Exception {
    org.opentripplanner.transit.model.network.Route result = subject.map(ROUTE);

    assertEquals("A:1", result.getId().toString());
    assertNotNull(result.getAgency());
    assertEquals(SHORT_NAME, result.getShortName());
    assertEquals(LONG_NAME, result.getLongName().toString());
    assertEquals(DESC, result.getDescription());
    assertEquals(ROUTE_TYPE, result.getGtfsType());
    assertEquals(TRANSIT_MODE, result.getMode());
    assertEquals(URL, result.getUrl());
    assertEquals(COLOR, result.getColor());
    assertEquals(TEXT_COLOR, result.getTextColor());
    assertEquals(BikeAccess.ALLOWED, result.getBikesAllowed());
    assertEquals(SORT_ORDER, result.getGtfsSortOrder());

    // We no longer read the non-standard brandingUrl from gtfs, but if it is supplied, it will not cause an error.
    Branding branding = result.getBranding();
    assertNull(branding);
  }

  @Test
  void testMapWithNulls() throws Exception {
    Route input = new Route();

    // id, agency, mode and name (short or long) is required.
    input.setId(ROUTE_ID);
    input.setAgency(AGENCY);
    input.setType(ROUTE_TYPE);
    input.setShortName(SHORT_NAME);

    org.opentripplanner.transit.model.network.Route result = subject.map(input);

    assertNotNull(result.getId());
    assertNotNull(result.getAgency());
    assertEquals(result.getShortName(), SHORT_NAME);
    assertNull(result.getLongName());
    assertNull(result.getDescription());
    assertEquals(ROUTE_TYPE.intValue(), (int) result.getGtfsType());
    assertEquals(TRANSIT_MODE, result.getMode());
    assertNull(result.getUrl());
    assertNull(result.getColor());
    assertNull(result.getTextColor());
    assertEquals(BikeAccess.UNKNOWN, result.getBikesAllowed());
    assertNull(result.getGtfsSortOrder());

    Branding branding = result.getBranding();
    assertNull(branding);
  }

  @Test
  void mapNetworkId() {
    Route input = new Route();

    input.setId(ROUTE_ID);
    input.setAgency(AGENCY);
    input.setType(ROUTE_TYPE);
    input.setShortName(SHORT_NAME);
    input.setNetworkId(NETWORK_ID);

    org.opentripplanner.transit.model.network.Route result = subject.map(input);

    assertEquals(
      List.of(new FeedScopedId(FEED_ID, NETWORK_ID)),
      result.getGroupsOfRoutes().stream().map(AbstractTransitEntity::getId).toList()
    );
  }

  @Test
  void carpool() {
    Route input = new Route();

    input.setId(ROUTE_ID);
    input.setAgency(AGENCY);
    input.setType(1551);
    input.setShortName(SHORT_NAME);

    var result = subject.map(input);

    assertEquals(TransitMode.CARPOOL, result.getMode());
  }

  /**
   * Mapping the same object twice, should return the same instance.
   */
  @Test
  void testMapCache() throws Exception {
    org.opentripplanner.transit.model.network.Route result1 = subject.map(ROUTE);
    org.opentripplanner.transit.model.network.Route result2 = subject.map(ROUTE);

    assertSame(result1, result2);
  }
}
