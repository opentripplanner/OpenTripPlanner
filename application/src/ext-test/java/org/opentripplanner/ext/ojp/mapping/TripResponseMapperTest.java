package org.opentripplanner.ext.ojp.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.ext.ojp.mapping.TripResponseMapper.OptionalFeature.LEG_PROJECTIONS;
import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.writeFile;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.File;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.ojp.schema.ojp.LegStructure;
import org.opentripplanner.ojp.schema.ojp.LegTrackStructure;
import org.opentripplanner.ojp.schema.ojp.OJP;
import org.opentripplanner.ojp.schema.ojp.OJPTripDeliveryStructure;
import org.opentripplanner.ojp.schema.ojp.TripResultStructure;
import org.opentripplanner.ojp.schema.siri.LocationStructure;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.test.support.ResourceLoader;

class TripResponseMapperTest implements PlanTestConstants {

  private static final ResourceLoader LOADER = ResourceLoader.of(TripResponseMapperTest.class);
  private static final File XML_FILE = LOADER.extTestResourceFile("trip-response.xml");
  private static final ZonedDateTime TIMESTAMP = ZonedDateTime.parse("2026-01-12T10:00:00Z");
  private static final LineString WALK_GEOMETRY = GeometryUtils.makeLineString(
    // x (longitude), y (latitude) pairs from stop A to stop B
    8.0,
    5.0,
    8.2,
    5.5,
    8.5,
    6.0
  );

  @Test
  void map() throws JAXBException {
    var ojp = map(itinerary(), Set.of());

    var xml = toXml(ojp);
    writeFile(XML_FILE, xml);
    assertFileEquals(xml, XML_FILE);
  }

  @Test
  void noLegTrackWithoutFeature() {
    var legs = legs(map(itineraryWithWalkGeometry(), Set.of()));

    assertNull(legs.get(0).getContinuousLeg().getLegTrack());
    assertNull(legs.get(1).getTimedLeg().getLegTrack());
  }

  @Test
  void transitLegTrackContainsAllCoordinates() {
    var itinerary = itinerary();
    var busLeg = itinerary.legs().get(1);

    var legs = legs(map(itinerary, Set.of(LEG_PROJECTIONS)));
    var legTrack = legs.get(1).getTimedLeg().getLegTrack();

    assertLegTrack(busLeg.legGeometry(), legTrack);
    assertEquals(busLeg.duration(), legTrack.getTrackSection().getFirst().getDuration());
  }

  @Test
  void streetLegTrackContainsAllCoordinates() {
    var itinerary = itineraryWithWalkGeometry();
    var walkLeg = itinerary.legs().getFirst();

    var legs = legs(map(itinerary, Set.of(LEG_PROJECTIONS)));
    var legTrack = legs.getFirst().getContinuousLeg().getLegTrack();

    assertLegTrack(WALK_GEOMETRY, legTrack);
    assertEquals(walkLeg.duration(), legTrack.getTrackSection().getFirst().getDuration());
  }

  @Test
  void streetLegWithoutGeometry() {
    var legs = legs(map(itinerary(), Set.of(LEG_PROJECTIONS)));

    assertNull(legs.getFirst().getContinuousLeg().getLegTrack());
  }

  private static void assertLegTrack(LineString expected, LegTrackStructure legTrack) {
    assertEquals(1, legTrack.getTrackSection().size());
    var positions = legTrack.getTrackSection().getFirst().getLinkProjection().getPosition();
    var expectedCoordinates = Arrays.stream(expected.getCoordinates())
      .map(c -> c.y + "," + c.x)
      .toList();
    var actualCoordinates = positions.stream().map(TripResponseMapperTest::latLon).toList();
    assertEquals(expectedCoordinates, actualCoordinates);
  }

  private static String latLon(LocationStructure l) {
    return l.getLatitude() + "," + l.getLongitude();
  }

  private static Itinerary itinerary() {
    return TestItineraryBuilder.newItinerary(A, T11_00)
      .walk(D3_m, B)
      .bus(1, T11_05, T11_15, C)
      .walk(D2_m, D)
      .build();
  }

  private static Itinerary itineraryWithWalkGeometry() {
    var itinerary = itinerary();
    var legs = itinerary
      .legs()
      .stream()
      .map(leg ->
        leg == itinerary.legs().getFirst()
          ? ((StreetLeg) leg).copyOf().withGeometry(WALK_GEOMETRY).build()
          : leg
      )
      .toList();
    return itinerary.copyOf().withLegs(legs).build();
  }

  private static OJP map(
    Itinerary itinerary,
    Set<TripResponseMapper.OptionalFeature> optionalFeatures
  ) {
    var tripPlan = new TripPlan(A, B, Instant.EPOCH, List.of(itinerary));
    var routingResponse = new RoutingResponse(tripPlan, null, null, null, null, null);
    var mapper = new TripResponseMapper(new DefaultFeedIdMapper(), optionalFeatures);
    return mapper.mapTripPlan(routingResponse, TIMESTAMP);
  }

  private static List<LegStructure> legs(OJP ojp) {
    var delivery = (OJPTripDeliveryStructure) ojp
      .getOJPResponse()
      .getServiceDelivery()
      .getAbstractFunctionalServiceDelivery()
      .getFirst()
      .getValue();
    return delivery
      .getRest()
      .stream()
      .map(JAXBElement::getValue)
      .filter(TripResultStructure.class::isInstance)
      .map(TripResultStructure.class::cast)
      .findFirst()
      .orElseThrow()
      .getTrip()
      .getLeg();
  }

  private static String toXml(OJP ojp) throws JAXBException {
    var marshaller = JAXBContext.newInstance(OJP.class).createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    var writer = new StringWriter();
    marshaller.marshal(ojp, writer);
    return writer.toString();
  }
}
