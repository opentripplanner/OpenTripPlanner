package org.opentripplanner.routing.algorithm.mapping;

import static au.com.origin.snapshots.SnapshotMatcher.expect;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import au.com.origin.snapshots.serializers.SerializerType;
import au.com.origin.snapshots.serializers.SnapshotSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.api.mapping.ItineraryMapper;
import org.opentripplanner.api.parameter.ApiRequestMode;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.parameter.Qualifier;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.TestUtils;
import org.opentripplanner.util.time.TimeUtils;

/**
 * A base class for creating snapshots test of itinerary generation using the Portland graph.
 *
 * If the snapshots need to be recreated run `mvn clean -Pclean-test-snapshots` to remove the
 * existing snapshots. When the tests are rerun new snapshots will be created.
 */
public abstract class SnapshotTestBase {

    private static final DateTimeFormatter apiDateFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final DateTimeFormatter apiTimeFormatter = DateTimeFormatter.ofPattern("H:mm%20a");
    private static final SnapshotSerializer snapshotSerializer = new SnapshotItinerarySerializer();
    private static final ItineraryMapper itineraryMapper = new ItineraryMapper(null, true);

    static final boolean verbose = Boolean.getBoolean("otp.test.verbose");

    protected Router router;

    public static void loadGraphBeforeClass() {
        ConstantsForTests.getInstance().getCachedPortlandGraph();
    }

    protected Router getRouter() {
        if (router == null) {
            Graph graph = ConstantsForTests.getInstance().getCachedPortlandGraph();

            router = new Router(graph, RouterConfig.DEFAULT);
            router.startup();
        }

        return router;
    }

    protected RoutingRequest createTestRequest(int year, int month, int day, int hour, int minute, int second) {
        Router router = getRouter();

        RoutingRequest request = router.defaultRoutingRequest.clone();
        request.setDateTime(TestUtils.dateInstant(router.graph.getTimeZone().getID(), year, month, day, hour, minute, second));
        request.maxTransfers = 6;
        request.numItineraries = 6;
        request.searchWindow = Duration.ofHours(5);

        return request;
    }

    protected void printItineraries(List<Itinerary> itineraries, long startMillis, long endMillis,
            TimeZone timeZone) {
        ZoneId zoneId = timeZone.toZoneId();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

        System.out.println("\n");

        for (int i = 0; i < itineraries.size(); i++) {
            Itinerary itinerary = itineraries.get(i);
            System.out
                    .printf("Itinerary %2d - duration: %s [%5d] (effective: %s [%5d]) - wait time: %d seconds, transit time: %d seconds\n",
                            i, TimeUtils.timeToStrCompact(itinerary.durationSeconds),
                            itinerary.durationSeconds,
                            TimeUtils.timeToStrCompact(itinerary.effectiveDurationSeconds()),
                            itinerary.effectiveDurationSeconds(), itinerary.waitingTimeSeconds,
                            itinerary.transitTimeSeconds);

            for (int j = 0; j < itinerary.legs.size(); j++) {
                Leg leg = itinerary.legs.get(j);
                String mode = leg.getMode().name().substring(0, 1);
                System.out.printf(" - leg %2d - %52.52s %9s --%s-> %-9s %-52.52s\n", j, leg.getFrom()
                                .toStringShort(),
                        dtf.format(leg.getStartTime().toInstant().atZone(zoneId)), mode,
                        dtf.format(leg.getEndTime().toInstant().atZone(zoneId)), leg.getTo().toStringShort());
            }

            System.out.println();
        }

        long printMillis = System.currentTimeMillis();

        System.out.println(
                "  Request duration: " + Duration.ofMillis(endMillis - startMillis).toMillis()
                        + " ms");
        System.out.println(
                "Request print time: " + Duration.ofMillis(printMillis - endMillis).toMillis()
                        + " ms");
    }

    protected void expectRequestResponseToMatchSnapshot(RoutingRequest request) {
        Router router = getRouter();

        List<Itinerary> itineraries = retrieveItineraries(request, router);

        logDebugInformationOnFailure(request, () -> expectItinerariesToMatchSnapshot(itineraries));
    }

    private List<Itinerary> retrieveItineraries(RoutingRequest request, Router router) {
        request.setRoutingContext(router.graph);

        long startMillis = System.currentTimeMillis();
        RoutingService routingService = new RoutingService(router.graph);
        RoutingResponse response = routingService.route(request, router);
        request.cleanup();

        List<Itinerary> itineraries = response.getTripPlan().itineraries;

        if (verbose) {
            printItineraries(itineraries, startMillis, System.currentTimeMillis(),
                    router.graph.getTimeZone());
        }
        return itineraries;
    }

    protected void expectArriveByToMatchDepartAtAndSnapshot(RoutingRequest request) {
        Router router = getRouter();

        RoutingRequest departAt = request.clone();
        List<Itinerary> departByItineraries = retrieveItineraries(departAt, router);

        logDebugInformationOnFailure(request, () -> assertFalse(departByItineraries.isEmpty()));

        logDebugInformationOnFailure(departAt, () -> expectItinerariesToMatchSnapshot(departByItineraries));

        RoutingRequest arriveBy = request.clone();
        arriveBy.setArriveBy(true);
        arriveBy.setDateTime(departByItineraries.get(0).lastLeg().getEndTime().toInstant());

        List<Itinerary> arriveByItineraries = retrieveItineraries(arriveBy, router);

        var departAtItinerary = departByItineraries.get(0);
        var arriveByItinerary = arriveByItineraries.get(0);

        logDebugInformationOnFailure(arriveBy, () -> assertEquals(
                asJsonString(itineraryMapper.mapItinerary(departAtItinerary)),
                asJsonString(itineraryMapper.mapItinerary(arriveByItinerary))
        ));
    }

    protected void expectItinerariesToMatchSnapshot(List<Itinerary> itineraries) {
        expect(itineraryMapper.mapItineraries(itineraries)).serializer(snapshotSerializer).toMatchSnapshot();
    }

    protected void logDebugInformationOnFailure(RoutingRequest request, Runnable task) {
        try {
            task.run();
        } catch (Throwable e) {
            System.err.println("\nTo recreate the snapshots used for verifying the tests run `mvn clean -Pclean-test-snapshots`.");
            System.err.println("The basic details of the request may be viewed using the debug client (not all parameters can be encoded in the link):");
            System.err.println("\n\t" + createDebugUrlForRequest(request));
            throw e;
        }
    }

    private static List<ApiRequestMode> mapModes(Collection<AllowedTransitMode> reqModes) {
        List<ApiRequestMode> result = new ArrayList<>();

        if(ApiRequestMode.TRANSIT.getTransitModes().equals(reqModes)) {
            return List.of(ApiRequestMode.TRANSIT);
        }

        for (AllowedTransitMode allowedTransitMode : reqModes) {
            Collection<AllowedTransitMode> allowedTransitModes = Set.of(allowedTransitMode);
            for (ApiRequestMode apiCandidate : ApiRequestMode.values()) {
                if(allowedTransitModes.equals(apiCandidate.getTransitModes())) {
                    result.add(apiCandidate);
                }
            }
        }
        return result;
    }

    private String createDebugUrlForRequest(RoutingRequest request) {
        var dateTime = Instant.ofEpochSecond(request.getDateTime().getEpochSecond())
                .atZone(getRouter().graph.getTimeZone().toZoneId())
                .toLocalDateTime();

        var transitModes = mapModes(request.modes.transitModes);

        var modes = Stream.concat(
                Stream.of(
                        asQualifiedMode(request.modes.directMode, false),
                        asQualifiedMode(request.modes.accessMode, false),
                        asQualifiedMode(request.modes.egressMode, true)
                )
                        .filter(Objects::nonNull)
                        .map(QualifiedMode::toString),
                transitModes.stream()
                        .map(ApiRequestMode::name)
        )
                .distinct()
                .collect(Collectors.joining(","));

        return String.format(
                "http://localhost:8080/?module=planner&fromPlace=%s&toPlace=%s&date=%s&time=%s&mode=%s&arriveBy=%s&wheelchair=%s",
                formatPlace(request.from),
                formatPlace(request.to),
                dateTime.toLocalDate().format(apiDateFormatter),
                dateTime.toLocalTime().format(apiTimeFormatter),
                modes,
                request.arriveBy,
                request.wheelchairAccessible
        );
    }

    private String formatPlace(GenericLocation location) {
        String formatted;
        if (location.stopId != null) {
            formatted = String.format("%s::%s", location.label, location.stopId);
        }
        else {
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
                return new QualifiedMode(ApiRequestMode.CAR, isEgress ? Qualifier.PICKUP : Qualifier.DROPOFF);
            case CAR_RENTAL:
                return new QualifiedMode(ApiRequestMode.CAR, Qualifier.RENT);
            case FLEXIBLE:
                return new QualifiedMode(ApiRequestMode.FLEX);
            default:
                return null;
        }
    }

    private static String asJsonString(Object object) {
        return snapshotSerializer.apply(new Object[] { object });
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

            objectMapper.setVisibility(
                    objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

            pp = new DefaultPrettyPrinter("") {
                @Override public DefaultPrettyPrinter createInstance() {
                    return this;
                }

                @Override public DefaultPrettyPrinter withSeparators(Separators separators) {
                    this._separators = separators;
                    this._objectFieldValueSeparatorWithSpaces =
                            separators.getObjectFieldValueSeparator() + " ";
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
            }
            catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to process snapshot JSON", e);
            }
        }

        @Override
        public String getOutputFormat() {
            return SerializerType.JSON.name();
        }
    }
}
