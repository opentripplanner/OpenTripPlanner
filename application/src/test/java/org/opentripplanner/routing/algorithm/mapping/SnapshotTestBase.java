package org.opentripplanner.routing.algorithm.mapping;

import static au.com.origin.snapshots.SnapshotMatcher.expect;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import au.com.origin.snapshots.serializers.SerializerType;
import au.com.origin.snapshots.serializers.SnapshotSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.TestServerContext;
import org.opentripplanner.api.parameter.ApiRequestMode;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.parameter.Qualifier;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.algorithm.mapping._support.mapping.ItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping._support.model.ApiLeg;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteRequestBuilder;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * A base class for creating snapshots test of itinerary generation using the Portland graph.
 * <p>
 * If the snapshots need to be recreated run `mvn clean -Pclean-test-snapshots` to remove the
 * existing snapshots. When the tests are rerun new snapshots will be created.
 */
public abstract class SnapshotTestBase {

  private static final DateTimeFormatter apiDateFormatter = DateTimeFormatter.ofPattern(
    "MM-dd-yyyy"
  );
  private static final DateTimeFormatter apiTimeFormatter = DateTimeFormatter.ofPattern("H:mm%20a");
  private static final SnapshotSerializer snapshotSerializer = new SnapshotItinerarySerializer();
  private static final ItineraryMapper itineraryMapper = new ItineraryMapper(Locale.ENGLISH, true);

  static final boolean verbose = Boolean.getBoolean("otp.test.verbose");

  protected OtpServerRequestContext serverContext;

  public static void loadGraphBeforeClass(boolean withElevation) {
    if (withElevation) {
      ConstantsForTests.getInstance().getCachedPortlandGraphWithElevation();
    } else {
      ConstantsForTests.getInstance().getCachedPortlandGraph();
    }
  }

  protected OtpServerRequestContext serverContext() {
    if (serverContext == null) {
      TestOtpModel model = getGraph();
      serverContext = TestServerContext.createServerContext(
        model.graph(),
        model.timetableRepository(),
        model.fareServiceFactory().makeFareService()
      );
    }

    return serverContext;
  }

  protected TestOtpModel getGraph() {
    return ConstantsForTests.getInstance().getCachedPortlandGraph();
  }

  protected RouteRequestBuilder createTestRequest(
    int year,
    int month,
    int day,
    int hour,
    int minute,
    int second
  ) {
    OtpServerRequestContext serverContext = serverContext();

    var builder = serverContext
      .defaultRouteRequest()
      .copyOf()
      .withDateTime(
        LocalDateTime.of(year, month, day, hour, minute, second)
          .atZone(ZoneId.of(serverContext.transitService().getTimeZone().getId()))
          .toInstant()
      )
      .withPreferences(pref -> pref.withTransfer(tx -> tx.withMaxTransfers(6)))
      .withNumItineraries(6)
      .withSearchWindow(Duration.ofHours(5));

    return builder;
  }

  protected void printItineraries(
    List<Itinerary> itineraries,
    long startMillis,
    long endMillis,
    ZoneId timeZone
  ) {
    System.out.println("\n");

    for (int i = 0; i < itineraries.size(); i++) {
      Itinerary itinerary = itineraries.get(i);
      System.out.printf(
        "Itinerary %2d - duration: %s [%5s] (effective: %s [%5s]) - wait time: %s, transit time: %s \n",
        i,
        TimeUtils.durationToStrCompact(itinerary.totalDuration()),
        itinerary.totalDuration(),
        TimeUtils.durationToStrCompact(itinerary.effectiveDuration()),
        itinerary.effectiveDuration(),
        itinerary.totalWaitingDuration(),
        itinerary.totalTransitDuration()
      );

      for (int j = 0; j < itinerary.legs().size(); j++) {
        Leg leg = itinerary.legs().get(j);
        String mode = (leg instanceof StreetLeg stLeg)
          ? stLeg.getMode().name().substring(0, 1)
          : "T";
        System.out.printf(
          " - leg %2d - %52.52s %9s --%s-> %-9s %-52.52s\n",
          j,
          leg.from().toStringShort(),
          ISO_LOCAL_TIME.format(leg.startTime().toInstant().atZone(timeZone)),
          mode,
          ISO_LOCAL_TIME.format(leg.endTime().toInstant().atZone(timeZone)),
          leg.to().toStringShort()
        );
      }

      System.out.println();
    }

    long printMillis = System.currentTimeMillis();

    System.out.println(
      "  Request duration: " + Duration.ofMillis(endMillis - startMillis).toMillis() + " ms"
    );
    System.out.println(
      "Request print time: " + Duration.ofMillis(printMillis - endMillis).toMillis() + " ms"
    );
  }

  protected void expectRequestResponseToMatchSnapshot(RouteRequest request) {
    List<Itinerary> itineraries = retrieveItineraries(request);

    logDebugInformationOnFailure(request, () -> expectItinerariesToMatchSnapshot(itineraries));
  }

  protected void expectArriveByToMatchDepartAtAndSnapshot(RouteRequest request) {
    List<Itinerary> departByItineraries = retrieveItineraries(request);

    logDebugInformationOnFailure(request, () -> assertFalse(departByItineraries.isEmpty()));

    logDebugInformationOnFailure(request, () ->
      expectItinerariesToMatchSnapshot(departByItineraries)
    );

    RouteRequest arriveBy = request
      .copyOf()
      .withArriveBy(true)
      .withDateTime(departByItineraries.get(0).legs().getLast().endTime().toInstant())
      .buildRequest();

    List<Itinerary> arriveByItineraries = retrieveItineraries(arriveBy);

    var departAtItinerary = departByItineraries.get(0);
    var arriveByItinerary = arriveByItineraries.get(0);

    logDebugInformationOnFailure(arriveBy, () ->
      assertEquals(
        asJsonString(itineraryMapper.mapItinerary(departAtItinerary)),
        asJsonString(itineraryMapper.mapItinerary(arriveByItinerary))
      )
    );
  }

  protected void expectItinerariesToMatchSnapshot(List<Itinerary> itineraries) {
    expect(itineraryMapper.mapItineraries(itineraries))
      .serializer(snapshotSerializer)
      .toMatchSnapshot();
  }

  protected void logDebugInformationOnFailure(RouteRequest request, Runnable task) {
    try {
      task.run();
    } catch (Throwable e) {
      System.err.println(
        "\nTo recreate the snapshots used for verifying the tests run `mvn clean -Pclean-test-snapshots`."
      );
      System.err.println(
        "The basic details of the request may be viewed using the debug client (not all parameters can be encoded in the link):"
      );
      System.err.println("\n\t" + createDebugUrlForRequest(request));
      throw e;
    }
  }

  private static List<ApiRequestMode> mapModes(Collection<MainAndSubMode> reqModes) {
    Set<TransitMode> transitModes = reqModes
      .stream()
      .map(MainAndSubMode::mainMode)
      .collect(Collectors.toSet());
    List<ApiRequestMode> result = new ArrayList<>();

    if (ApiRequestMode.TRANSIT.getTransitModes().equals(transitModes)) {
      return List.of(ApiRequestMode.TRANSIT);
    }

    for (TransitMode it : transitModes) {
      for (ApiRequestMode apiCandidate : ApiRequestMode.values()) {
        if (apiCandidate.getTransitModes().contains(it)) {
          result.add(apiCandidate);
        }
      }
    }
    return result;
  }

  private static String asJsonString(Object object) {
    return snapshotSerializer.apply(new Object[] { object });
  }

  private List<Itinerary> retrieveItineraries(RouteRequest request) {
    long startMillis = System.currentTimeMillis();
    RoutingResponse response = serverContext.routingService().route(request);

    List<Itinerary> itineraries = response.getTripPlan().itineraries;

    if (verbose) {
      printItineraries(
        itineraries,
        startMillis,
        System.currentTimeMillis(),
        serverContext.transitService().getTimeZone()
      );
    }
    return itineraries;
  }

  private String createDebugUrlForRequest(RouteRequest request) {
    var dateTime = Instant.ofEpochSecond(request.dateTime().getEpochSecond())
      .atZone(serverContext().transitService().getTimeZone())
      .toLocalDateTime();

    // TODO: 2022-12-20 filters: there should not be more than one filter but technically this is not right
    List<MainAndSubMode> transportModes = new ArrayList<>();
    var filter = request.journey().transit().filters().get(0);
    if (filter instanceof TransitFilterRequest filterRequest) {
      transportModes = filterRequest.select().get(0).transportModes();
    } else if (filter instanceof AllowAllTransitFilter) {
      transportModes = MainAndSubMode.all();
    }

    var transitModes = mapModes(transportModes);

    var modes = Stream.concat(
      Stream.of(
        asQualifiedMode(request.journey().direct().mode(), false),
        asQualifiedMode(request.journey().access().mode(), false),
        asQualifiedMode(request.journey().egress().mode(), true)
      )
        .filter(Objects::nonNull)
        .map(QualifiedMode::toString),
      transitModes.stream().map(ApiRequestMode::name)
    )
      .distinct()
      .collect(Collectors.joining(","));

    return String.format(
      "http://localhost:8080/?module=planner&fromPlace=%s&toPlace=%s&date=%s&time=%s&mode=%s&arriveBy=%s&wheelchair=%s",
      formatPlace(request.from()),
      formatPlace(request.to()),
      dateTime.toLocalDate().format(apiDateFormatter),
      dateTime.toLocalTime().format(apiTimeFormatter),
      modes,
      request.arriveBy(),
      request.preferences().wheelchair()
    );
  }

  private String formatPlace(GenericLocation location) {
    String formatted;
    if (location.stopId != null) {
      formatted = String.format("%s::%s", location.label, location.stopId);
    } else {
      formatted = String.format("%s::%s,%s", location.label, location.lat, location.lng);
    }
    return URLEncoder.encode(formatted, StandardCharsets.UTF_8);
  }

  private QualifiedMode asQualifiedMode(StreetMode streetMode, boolean isEgress) {
    if (streetMode == null) {
      return null;
    }

    switch (streetMode) {
      case WALK:
        return new QualifiedMode(ApiRequestMode.WALK);
      case BIKE:
        return new QualifiedMode(ApiRequestMode.BICYCLE);
      case BIKE_TO_PARK:
        return new QualifiedMode(ApiRequestMode.BICYCLE, Qualifier.PARK);
      case BIKE_RENTAL:
        return new QualifiedMode(ApiRequestMode.BICYCLE, Qualifier.RENT);
      case SCOOTER_RENTAL:
        return new QualifiedMode(ApiRequestMode.SCOOTER, Qualifier.RENT);
      case CAR:
        return new QualifiedMode(ApiRequestMode.CAR);
      case CAR_TO_PARK:
        return new QualifiedMode(ApiRequestMode.CAR, Qualifier.PARK);
      case CAR_PICKUP:
        return new QualifiedMode(
          ApiRequestMode.CAR,
          isEgress ? Qualifier.PICKUP : Qualifier.DROPOFF
        );
      case CAR_RENTAL:
        return new QualifiedMode(ApiRequestMode.CAR, Qualifier.RENT);
      case FLEXIBLE:
        return new QualifiedMode(ApiRequestMode.FLEX);
      default:
        return null;
    }
  }

  private static class SnapshotItinerarySerializer implements SnapshotSerializer {

    private final ObjectMapper objectMapper;
    private final DefaultPrettyPrinter pp;

    private SnapshotItinerarySerializer() {
      objectMapper = new ObjectMapper();
      objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
      objectMapper.enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);
      objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
      objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      objectMapper.registerModule(new JavaTimeModule());
      objectMapper.registerModule(new Jdk8Module());

      objectMapper.addMixIn(ApiLeg.class, ApiLegMixin.class);

      pp = new DefaultPrettyPrinter("") {
        @Override
        public DefaultPrettyPrinter withSeparators(Separators separators) {
          this._separators = separators;
          this._objectFieldValueSeparatorWithSpaces =
            separators.getObjectFieldValueSeparator() + " ";
          return this;
        }

        @Override
        public DefaultPrettyPrinter createInstance() {
          return this;
        }
      };

      DefaultPrettyPrinter.Indenter lfOnlyIndenter = new DefaultIndenter("  ", "\n");
      pp.indentArraysWith(lfOnlyIndenter);
      pp.indentObjectsWith(lfOnlyIndenter);
    }

    @Override
    public String apply(Object[] objects) {
      try {
        return objectMapper.writer(pp).writeValueAsString(objects);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Failed to process snapshot JSON", e);
      }
    }

    @Override
    public String getOutputFormat() {
      return SerializerType.JSON.name();
    }
  }

  /**
   * To exclude {@link ApiLeg#getDuration()} from being deserialized because the returned number
   * is non-constant making it impossible to assert.
   */
  private abstract static class ApiLegMixin {

    @JsonIgnore
    abstract double getDuration();
  }
}
