package org.opentripplanner.ext.ojp.mapping;

import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.writeFile;

import de.vdv.ojp20.OJP;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.File;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.test.support.ResourceLoader;

class TripResponseMapperTest implements PlanTestConstants {

  private static final ResourceLoader LOADER = ResourceLoader.of(TripResponseMapperTest.class);
  private static final File XML_FILE = LOADER.extTestResourceFile("trip-response.xml");

  @Test
  void map() throws JAXBException {
    Itinerary itinerary = TestItineraryBuilder.newItinerary(A, T11_00)
      .walk(D3_m, B)
      .bus(1, T11_05, T11_15, C)
      .walk(D2_m, D)
      .build();

    // Create routing response with the itinerary
    var tripPlan = new TripPlan(A, B, Instant.EPOCH, List.of(itinerary));
    var routingResponse = new RoutingResponse(tripPlan, null, null, null, null, null);

    // Map to OJP
    var idMapper = new DefaultFeedIdMapper();
    var mapper = new TripResponseMapper(idMapper, Set.of());
    ZonedDateTime timestamp = ZonedDateTime.parse("2026-01-12T10:00:00Z");
    OJP ojp = mapper.mapTripPlan(routingResponse, timestamp);

    // Serialize to XML using JAXB
    JAXBContext jaxbContext = JAXBContext.newInstance(OJP.class);
    var marshaller = jaxbContext.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    StringWriter writer = new StringWriter();
    marshaller.marshal(ojp, writer);
    String xml = writer.toString();

    writeFile(XML_FILE, xml);
    assertFileEquals(xml, XML_FILE);
  }
}
