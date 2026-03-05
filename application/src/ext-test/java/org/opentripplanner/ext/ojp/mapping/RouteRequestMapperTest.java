package org.opentripplanner.ext.ojp.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import de.vdv.ojp20.LineDirectionFilterStructure;
import de.vdv.ojp20.ModeAndModeOfOperationFilterStructure;
import de.vdv.ojp20.OJPTripRequestStructure;
import de.vdv.ojp20.OperatorFilterStructure;
import de.vdv.ojp20.PersonalModesEnumeration;
import de.vdv.ojp20.PlaceContextStructure;
import de.vdv.ojp20.PlaceRefStructure;
import de.vdv.ojp20.StopPlaceRefStructure;
import de.vdv.ojp20.TripParamStructure;
import de.vdv.ojp20.siri.LineDirectionStructure;
import de.vdv.ojp20.siri.LineRefStructure;
import de.vdv.ojp20.siri.LocationStructure;
import de.vdv.ojp20.siri.OperatorRefStructure;
import de.vdv.ojp20.siri.StopPointRefStructure;
import de.vdv.ojp20.siri.VehicleModesOfTransportEnumeration;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.StreetMode;

class RouteRequestMapperTest {

  private final RouteRequestMapper mapper = new RouteRequestMapper(
    new DefaultFeedIdMapper(),
    RouteRequest.defaultValue()
  );

  @Test
  void mapWithCoordinates() {
    var tripRequest = baseRequest();

    var routeRequest = mapper.map(tripRequest);

    assertEquals(47.3769, routeRequest.from().lat);
    assertEquals(8.5417, routeRequest.from().lng);
    assertEquals(46.9480, routeRequest.to().lat);
    assertEquals(7.4474, routeRequest.to().lng);
    assertTransitFilters("[ALLOW_ALL]", routeRequest);

    assertEquals(StreetMode.WALK, routeRequest.journey().access().mode());
  }

  @Test
  void mapWithStopPlaceRef() {
    var tripRequest = new OJPTripRequestStructure()
      .withOrigin(stopPlaceRef("F:stop1"))
      .withDestination(stopPlaceRef("F:stop2"));

    var routeRequest = mapper.map(tripRequest);

    assertEquals(id("stop1"), routeRequest.from().stopId);
    assertEquals(id("stop2"), routeRequest.to().stopId);
  }

  @Test
  void mapWithStopPointRef() {
    var tripRequest = new OJPTripRequestStructure()
      .withOrigin(stopPointRef("F:stopPoint1"))
      .withDestination(stopPointRef("F:stopPoint2"));

    var routeRequest = mapper.map(tripRequest);

    assertNotNull(routeRequest.to());
    assertEquals(id("stopPoint1"), routeRequest.from().stopId);
    assertEquals(id("stopPoint2"), routeRequest.to().stopId);
  }

  @Test
  void mapWithMixedLocationTypes() {
    var tripRequest = new OJPTripRequestStructure()
      .withOrigin(geoPosition(47.3769, 8.5417))
      .withDestination(stopPlaceRef("F:stop1"));

    var routeRequest = mapper.map(tripRequest);

    assertEquals(47.3769, routeRequest.from().lat);
    assertEquals(8.5417, routeRequest.from().lng);
    assertEquals(id("stop1"), routeRequest.to().stopId);
  }

  @Test
  void throwsExceptionForMultipleOrigins() {
    var tripRequest = new OJPTripRequestStructure()
      .withOrigin(geoPosition(47.3769, 8.5417), geoPosition(47.5, 8.5))
      .withDestination(geoPosition(46.9480, 7.4474));

    var exception = assertThrows(IllegalArgumentException.class, () -> mapper.map(tripRequest));
    assertThat(exception.getMessage()).contains("one origin and one destination");
  }

  @Test
  void throwsExceptionForMultipleDestinations() {
    var tripRequest = new OJPTripRequestStructure()
      .withOrigin(geoPosition(47.3769, 8.5417))
      .withDestination(geoPosition(46.9480, 7.4474), geoPosition(46.5, 7.5));

    var exception = assertThrows(IllegalArgumentException.class, () -> mapper.map(tripRequest));
    assertThat(exception.getMessage()).contains("one origin and one destination");
  }

  @Test
  void throwsExceptionForNoOrigin() {
    var tripRequest = new OJPTripRequestStructure().withDestination(geoPosition(46.9480, 7.4474));

    var exception = assertThrows(IllegalArgumentException.class, () -> mapper.map(tripRequest));
    assertThat(exception.getMessage()).contains("one origin and one destination");
  }

  @Test
  void throwsExceptionForNoDestination() {
    var tripRequest = new OJPTripRequestStructure().withOrigin(geoPosition(47.3769, 8.5417));

    var exception = assertThrows(IllegalArgumentException.class, () -> mapper.map(tripRequest));
    assertThat(exception.getMessage()).contains("one origin and one destination");
  }

  @Test
  void throwsExceptionForNullPlaceRef() {
    var tripRequest = new OJPTripRequestStructure()
      .withOrigin(new PlaceContextStructure())
      .withDestination(geoPosition(46.9480, 7.4474));

    var exception = assertThrows(IllegalArgumentException.class, () -> mapper.map(tripRequest));
    assertThat(exception.getMessage()).contains("PlaceContext of origin is empty");
  }

  @Test
  void throwsExceptionForEmptyPlaceRef() {
    var tripRequest = new OJPTripRequestStructure()
      .withOrigin(geoPosition(47.3769, 8.5417))
      .withDestination(new PlaceContextStructure().withPlaceRef(new PlaceRefStructure()));

    var exception = assertThrows(IllegalArgumentException.class, () -> mapper.map(tripRequest));
    assertThat(exception.getMessage()).contains(
      "PlaceContext of destination contains neither stop reference nor coordinates"
    );
  }

  @Test
  void mapNumberOfResults() {
    var tripRequest = baseRequest().withParams(new TripParamStructure().withNumberOfResults(5));

    var routeRequest = mapper.map(tripRequest);

    assertEquals(5, routeRequest.numItineraries());
  }

  @Test
  void excludeMode() {
    var tripRequest = new OJPTripRequestStructure()
      .withOrigin(geoPosition(47.3769, 8.5417))
      .withDestination(geoPosition(46.9480, 7.4474))
      .withParams(
        new TripParamStructure().withModeAndModeOfOperationFilter(
          new ModeAndModeOfOperationFilterStructure()
            .withExclude(true)
            .withPtMode(VehicleModesOfTransportEnumeration.RAIL)
        )
      );

    var routeRequest = mapper.map(tripRequest);

    assertTransitFilters("[(not: [(transportModes: [RAIL])])]", routeRequest);
  }

  private static List<Arguments> transitFilterCases() {
    return List.of(
      Arguments.of(
        "[(select: [(transportModes: EMPTY, agencies: [F:agency1])])]",
        new TripParamStructure().withOperatorFilter(
          new OperatorFilterStructure()
            .withExclude(false)
            .withOperatorRef(List.of(new OperatorRefStructure().withValue("F:agency1")))
        )
      ),
      Arguments.of(
        "[(not: [(transportModes: EMPTY, agencies: [F:agency1])])]",
        new TripParamStructure().withOperatorFilter(
          new OperatorFilterStructure()
            .withExclude(true)
            .withOperatorRef(List.of(new OperatorRefStructure().withValue("F:agency1")))
        )
      ),
      Arguments.of(
        "[(select: [(transportModes: EMPTY, routes: [F:route1])])]",
        new TripParamStructure().withLineFilter(
          new LineDirectionFilterStructure()
            .withExclude(false)
            .withLine(
              (new LineDirectionStructure().withLineRef(
                  new LineRefStructure().withValue("F:route1")
                ))
            )
        )
      ),
      Arguments.of(
        "[(not: [(transportModes: EMPTY, routes: [F:agency1])])]",
        new TripParamStructure().withLineFilter(
          new LineDirectionFilterStructure()
            .withExclude(true)
            .withLine(
              (new LineDirectionStructure().withLineRef(
                  new LineRefStructure().withValue("F:agency1")
                ))
            )
        )
      ),
      Arguments.of(
        "[(not: [(transportModes: EMPTY, routes: [B:route2, A:route1])])]",
        new TripParamStructure().withLineFilter(
          new LineDirectionFilterStructure()
            .withExclude(true)
            .withLine(
              new LineDirectionStructure().withLineRef(
                new LineRefStructure().withValue("A:route1")
              ),
              new LineDirectionStructure().withLineRef(new LineRefStructure().withValue("B:route2"))
            )
        )
      )
    );
  }

  @ParameterizedTest
  @MethodSource("transitFilterCases")
  void transitFilter(String expectedFilter, TripParamStructure tripParamStructure) {
    var tripRequest = baseRequest().withParams(tripParamStructure);
    var routeRequest = mapper.map(tripRequest);
    assertTransitFilters(expectedFilter, routeRequest);
  }

  @Test
  void mapAdditionalTransferTime() {
    var tripRequest = baseRequest().withParams(
      new TripParamStructure().withAdditionalTransferTime(Duration.ofMinutes(10))
    );

    var routeRequest = mapper.map(tripRequest);

    assertEquals(Duration.ofMinutes(10), routeRequest.preferences().transfer().slack());
  }

  private static List<Arguments> personalModeCase() {
    return List.of(
      Arguments.of(PersonalModesEnumeration.FOOT, StreetMode.WALK),
      Arguments.of(PersonalModesEnumeration.CAR, StreetMode.CAR)
    );
  }

  @ParameterizedTest
  @MethodSource("personalModeCase")
  void personalMode(PersonalModesEnumeration personalMode, StreetMode expectedMode) {
    var tripRequest = baseRequest().withParams(
      new TripParamStructure().withModeAndModeOfOperationFilter(
        new ModeAndModeOfOperationFilterStructure()
          .withExclude(false)
          .withPersonalMode(personalMode)
      )
    );

    var routeRequest = mapper.map(tripRequest);

    assertEquals(expectedMode, routeRequest.journey().access().mode());
  }

  private static Stream<TripParamStructure> invalidPersonalModeCases() {
    return Stream.of(
      new TripParamStructure().withModeAndModeOfOperationFilter(
        new ModeAndModeOfOperationFilterStructure()
          .withExclude(true)
          .withPersonalMode(PersonalModesEnumeration.BICYCLE)
      ),
      new TripParamStructure().withModeAndModeOfOperationFilter(
        new ModeAndModeOfOperationFilterStructure()
          .withExclude(false)
          .withPersonalMode(PersonalModesEnumeration.BICYCLE, PersonalModesEnumeration.CAR)
      ),
      new TripParamStructure().withModeAndModeOfOperationFilter(
        new ModeAndModeOfOperationFilterStructure()
          .withExclude(false)
          .withPersonalMode(PersonalModesEnumeration.BICYCLE),
        new ModeAndModeOfOperationFilterStructure()
          .withExclude(false)
          .withPersonalMode(PersonalModesEnumeration.FOOT)
      )
    );
  }

  @ParameterizedTest
  @MethodSource("invalidPersonalModeCases")
  void throwsExceptionForUnsupportedPersonalMode(TripParamStructure tripParamStructure) {
    var tripRequest = baseRequest().withParams(tripParamStructure);

    assertThrows(IllegalArgumentException.class, () -> mapper.map(tripRequest));
  }

  private static OJPTripRequestStructure baseRequest() {
    return new OJPTripRequestStructure()
      .withOrigin(geoPosition(47.3769, 8.5417))
      .withDestination(geoPosition(46.9480, 7.4474));
  }

  private static void assertTransitFilters(String expected, RouteRequest routeRequest) {
    assertEquals(expected, routeRequest.journey().transit().filters().toString());
  }

  private static PlaceContextStructure geoPosition(double lat, double lng) {
    return new PlaceContextStructure().withPlaceRef(
      new PlaceRefStructure().withGeoPosition(
        new LocationStructure().withLatitude(lat).withLongitude(lng)
      )
    );
  }

  private static PlaceContextStructure stopPlaceRef(String value) {
    return new PlaceContextStructure().withPlaceRef(
      new PlaceRefStructure().withStopPlaceRef(new StopPlaceRefStructure().withValue(value))
    );
  }

  private static PlaceContextStructure stopPointRef(String value) {
    return new PlaceContextStructure().withPlaceRef(
      new PlaceRefStructure().withStopPointRef(new StopPointRefStructure().withValue(value))
    );
  }
}
