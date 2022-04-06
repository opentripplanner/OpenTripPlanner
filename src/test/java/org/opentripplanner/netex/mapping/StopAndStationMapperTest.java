package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.netex.index.hierarchy.HierarchicalVersionMapById;
import org.rutebanken.netex.model.AccessibilityAssessment;
import org.rutebanken.netex.model.AccessibilityLimitation;
import org.rutebanken.netex.model.AccessibilityLimitations_RelStructure;
import org.rutebanken.netex.model.LimitationStatusEnumeration;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.VehicleModeEnumeration;

public class StopAndStationMapperTest {

  @Test
  public void testWheelChairBoarding() {
    var stopPlace = createStopPlace(
      "ST:StopPlace:1",
      "Lunce C",
      "1",
      55.707005,
      13.186816,
      VehicleModeEnumeration.BUS
    );

    // Create on quay with access, one without, and one with NULL
    var quay1 = createQuay("ST:Quay:1", "Quay1", "1", 55.706063, 13.186708, "a");
    var quay2 = createQuay("ST:Quay:2", "Quay2", "1", 55.706775, 13.186482, "a");

    var quay3 = createQuay("ST:Quay:3", "Quay3", "1", 55.707330, 13.186397, "a");

    var quay4 = createQuay("ST:Quay:4", "Quay4", "1", 55.707330, 13.186397, "a");

    quay1.withAccessibilityAssessment(
      createAccessibilityAssessment(LimitationStatusEnumeration.TRUE)
    );

    quay2.withAccessibilityAssessment(
      createAccessibilityAssessment(LimitationStatusEnumeration.FALSE)
    );

    stopPlace.setQuays(
      new Quays_RelStructure()
        .withQuayRefOrQuay(quay1)
        .withQuayRefOrQuay(quay2)
        .withQuayRefOrQuay(quay3)
    );

    var stopPlaceById = new HierarchicalVersionMapById<StopPlace>();
    stopPlaceById.add(stopPlace);

    var stopAndStationMapper = new StopAndStationMapper(
      MappingSupport.ID_FACTORY,
      new HierarchicalVersionMapById<>(),
      null,
      new DataImportIssueStore(false)
    );

    stopAndStationMapper.mapParentAndChildStops(List.of(stopPlace));

    var stops = stopAndStationMapper.resultStops;

    assertEquals(3, stops.size(), "Stops.size must be 3 found " + stops.size());

    assertWheelChairBoarding("ST:Quay:1", WheelChairBoarding.POSSIBLE, stops);
    assertWheelChairBoarding("ST:Quay:2", WheelChairBoarding.NOT_POSSIBLE, stops);
    assertWheelChairBoarding("ST:Quay:3", WheelChairBoarding.NO_INFORMATION, stops);

    // Now test with AccessibilityAssessment set on StopPlace (should be default)
    stopPlace.withAccessibilityAssessment(
      createAccessibilityAssessment(LimitationStatusEnumeration.TRUE)
    );

    // Add quay with no AccessibilityAssessment, then it should take default from stopPlace
    stopPlace.getQuays().withQuayRefOrQuay(quay4);

    stopAndStationMapper.mapParentAndChildStops(List.of(stopPlace));

    assertEquals(4, stops.size(), "stops.size must be 4 found " + stops.size());
    assertWheelChairBoarding("ST:Quay:4", WheelChairBoarding.POSSIBLE, stops);
  }

  @Test
  public void mapStopPlaceAndQuays() {
    Collection<StopPlace> stopPlaces = new ArrayList<>();

    StopPlace stopPlaceNew = createStopPlace(
      "NSR:StopPlace:1",
      "Oslo S",
      "2",
      59.909584,
      10.755165,
      VehicleModeEnumeration.TRAM
    );

    StopPlace stopPlaceOld = createStopPlace(
      "NSR:StopPlace:1",
      "Oslo S",
      "1",
      59.909584,
      10.755165,
      VehicleModeEnumeration.TRAM
    );

    stopPlaces.add(stopPlaceNew);
    stopPlaces.add(stopPlaceOld);

    Quay quay1a = createQuay("NSR:Quay:1", "", "1", 59.909323, 10.756205, "a");

    Quay quay1b = createQuay("NSR:Quay:1", "", "2", 59.909911, 10.753008, "A");

    Quay quay2 = createQuay("NSR:Quay:2", "", "1", 59.909911, 10.753008, "B");

    Quay quay3 = createQuay("NSR:Quay:3", "", "1", 59.909911, 10.753008, "C");

    stopPlaceNew.setQuays(
      new Quays_RelStructure().withQuayRefOrQuay(quay1b).withQuayRefOrQuay(quay2)
    );

    stopPlaceOld.setQuays(
      new Quays_RelStructure().withQuayRefOrQuay(quay1a).withQuayRefOrQuay(quay3)
    );

    HierarchicalVersionMapById<Quay> quaysById = new HierarchicalVersionMapById<>();
    quaysById.add(quay1a);
    quaysById.add(quay1a);
    quaysById.add(quay2);
    quaysById.add(quay3);

    StopAndStationMapper stopMapper = new StopAndStationMapper(
      MappingSupport.ID_FACTORY,
      quaysById,
      null,
      new DataImportIssueStore(false)
    );

    stopMapper.mapParentAndChildStops(stopPlaces);

    Collection<Stop> stops = stopMapper.resultStops;
    Collection<Station> stations = stopMapper.resultStations;

    assertEquals(3, stops.size());
    assertEquals(1, stations.size());

    Station parentStop = stations
      .stream()
      .filter(s -> s.getId().getId().equals("NSR:StopPlace:1"))
      .findFirst()
      .get();
    Stop childStop1 = stops
      .stream()
      .filter(s -> s.getId().getId().equals("NSR:Quay:1"))
      .findFirst()
      .get();
    Stop childStop2 = stops
      .stream()
      .filter(s -> s.getId().getId().equals("NSR:Quay:2"))
      .findFirst()
      .get();
    Stop childStop3 = stops
      .stream()
      .filter(s -> s.getId().getId().equals("NSR:Quay:3"))
      .findFirst()
      .get();

    assertEquals("NSR:StopPlace:1", parentStop.getId().getId());
    assertEquals("NSR:Quay:1", childStop1.getId().getId());
    assertEquals("NSR:Quay:2", childStop2.getId().getId());
    assertEquals("NSR:Quay:3", childStop3.getId().getId());

    assertEquals(59.909911, childStop1.getLat(), 0.0001);
    assertEquals(10.753008, childStop1.getLon(), 0.0001);
    assertEquals("A", childStop1.getCode());
  }

  private static StopPlace createStopPlace(
    String id,
    String name,
    String version,
    Double lat,
    Double lon,
    VehicleModeEnumeration transportMode
  ) {
    return new StopPlace()
      .withName(createMLString(name))
      .withVersion(version)
      .withId(id)
      .withCentroid(createSimplePoint(lat, lon))
      .withTransportMode(transportMode);
  }

  private static Quay createQuay(
    String id,
    String name,
    String version,
    Double lat,
    Double lon,
    String platformCode
  ) {
    return new Quay()
      .withName(createMLString(name))
      .withId(id)
      .withVersion(version)
      .withPublicCode(platformCode)
      .withCentroid(createSimplePoint(lat, lon));
  }

  private static MultilingualString createMLString(String name) {
    return new MultilingualString().withValue(name);
  }

  private static SimplePoint_VersionStructure createSimplePoint(Double lat, Double lon) {
    return new SimplePoint_VersionStructure()
      .withLocation(
        new LocationStructure().withLatitude(new BigDecimal(lat)).withLongitude(new BigDecimal(lon))
      );
  }

  /**
   * Utility function to assert WheelChairBoarding from Stop.
   *
   * @param quayId   ID to find corresponding Stop
   * @param expected Expected WheelChairBoarding value in assertion
   * @param stops    Find correct stop from list
   */
  private void assertWheelChairBoarding(
    String quayId,
    WheelChairBoarding expected,
    List<Stop> stops
  ) {
    var wheelChairBoarding = stops
      .stream()
      .filter(s -> s.getId().getId().equals(quayId))
      .findAny()
      .map(Stop::getWheelchairBoarding)
      .orElse(null);

    assertNotNull(wheelChairBoarding, "wheelChairBoarding must not be null");
    assertEquals(
      expected,
      wheelChairBoarding,
      () ->
        "wheelChairBoarding should be " +
        expected +
        " found " +
        wheelChairBoarding +
        " for quayId = " +
        quayId
    );
  }

  /**
   * Utility function to create AccessibilityAssessment and inject correct value.
   *
   * @param wheelChairAccess Value to WheelChairAccess
   * @return AccessibilityAssessment with injected value
   */
  private AccessibilityAssessment createAccessibilityAssessment(
    LimitationStatusEnumeration wheelChairAccess
  ) {
    var accessibilityLimitation = new AccessibilityLimitation()
      .withWheelchairAccess(wheelChairAccess);

    var limitations = new AccessibilityLimitations_RelStructure()
      .withAccessibilityLimitation(accessibilityLimitation);

    return new AccessibilityAssessment().withLimitations(limitations);
  }
}
