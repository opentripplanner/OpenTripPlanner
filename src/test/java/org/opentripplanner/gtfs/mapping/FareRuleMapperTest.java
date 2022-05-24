package org.opentripplanner.gtfs.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import org.junit.Test;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.FareRule;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.graph_builder.DataImportIssueStore;

public class FareRuleMapperTest {

  private static final String FEED_ID = "FEED";

  private static final org.onebusaway.gtfs.model.FareRule FARE_RULE = new org.onebusaway.gtfs.model.FareRule();

  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

  private static final Integer ID = 45;

  private static final String CONTAINS_ID = "Contains Id";

  private static final String DESTINATION_ID = "Destination Id";

  private static final FareAttribute FARE_ATTRIBUTE = new FareAttribute();

  private static final String ORIGIN_ID = "Origin Id";

  private static final Route ROUTE = new Route();
  private final FareRuleMapper subject = new FareRuleMapper(
    new RouteMapper(new AgencyMapper(FEED_ID), new DataImportIssueStore(false)),
    new FareAttributeMapper()
  );

  static {
    var agency = new Agency();
    agency.setId("Agency:1");
    agency.setName("Agency 1");

    FARE_ATTRIBUTE.setId(AGENCY_AND_ID);
    ROUTE.setId(AGENCY_AND_ID);
    ROUTE.setAgency(agency);
    ROUTE.setType(3);
    ROUTE.setShortName("R1");

    FARE_RULE.setId(ID);
    FARE_RULE.setContainsId(CONTAINS_ID);
    FARE_RULE.setDestinationId(DESTINATION_ID);
    FARE_RULE.setFare(FARE_ATTRIBUTE);
    FARE_RULE.setOriginId(ORIGIN_ID);
    FARE_RULE.setRoute(ROUTE);
  }

  @Test
  public void testMapCollection() throws Exception {
    assertNull(subject.map((Collection<FareRule>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(FARE_RULE)).size());
  }

  @Test
  public void testMap() throws Exception {
    org.opentripplanner.model.FareRule result = subject.map(FARE_RULE);

    assertEquals(CONTAINS_ID, result.getContainsId());
    assertEquals(DESTINATION_ID, result.getDestinationId());
    assertEquals(ORIGIN_ID, result.getOriginId());
    assertNotNull(result.getFare());
    assertNotNull(result.getRoute());
  }

  @Test
  public void testMapWithNulls() throws Exception {
    org.opentripplanner.model.FareRule result = subject.map(new FareRule());

    assertNull(result.getContainsId());
    assertNull(result.getDestinationId());
    assertNull(result.getOriginId());
    assertNull(result.getFare());
    assertNull(result.getRoute());
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  public void testMapCache() throws Exception {
    org.opentripplanner.model.FareRule result1 = subject.map(FARE_RULE);
    org.opentripplanner.model.FareRule result2 = subject.map(FARE_RULE);

    assertSame(result1, result2);
  }
}
