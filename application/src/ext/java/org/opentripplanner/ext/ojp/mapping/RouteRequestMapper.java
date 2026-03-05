package org.opentripplanner.ext.ojp.mapping;

import static java.lang.Boolean.FALSE;
import static java.time.ZoneOffset.UTC;
import static org.opentripplanner.ext.ojp.mapping.TripResponseMapper.OptionalFeature.INTERMEDIATE_STOPS;

import de.vdv.ojp20.ModeAndModeOfOperationFilterStructure;
import de.vdv.ojp20.OJPTripRequestStructure;
import de.vdv.ojp20.PlaceContextStructure;
import de.vdv.ojp20.TripParamStructure;
import de.vdv.ojp20.UseRealtimeDataEnumeration;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteRequestBuilder;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.model.basic.MainAndSubMode;

public class RouteRequestMapper {

  private final FeedScopedIdMapper idMapper;
  private final RouteRequest defaultRequest;
  private final FilterMapper filterMapper;

  public RouteRequestMapper(FeedScopedIdMapper idMapper, RouteRequest defaultRequest) {
    this.idMapper = idMapper;
    this.filterMapper = new FilterMapper(idMapper);
    this.defaultRequest = defaultRequest;
  }

  public RouteRequest map(OJPTripRequestStructure tr) {
    if (tr.getDestination().size() == 1 && tr.getOrigin().size() == 1) {
      var origin = tr.getOrigin().getFirst();
      var destination = tr.getDestination().getFirst();
      var from = toGenericLocation(origin, "origin");
      var to = toGenericLocation(destination, "destination");

      var builder = defaultRequest
        .copyOf()
        .withFrom(from)
        .withTo(to)
        .withNumItineraries(numItineraries(tr))
        .withJourney(j -> j.withModes(extractAccessAndEgressModes(tr)))
        .withPreferences(p -> {
          p.withTransit(t -> t.withIgnoreRealtimeUpdates(ignoreRealtime(tr)));
          transferSlack(tr).ifPresent(slack ->
            p.withTransfer(transfer -> transfer.withSlack(slack))
          );
        });

      addTime(origin, destination, builder);
      addExcludedModes(tr, builder);
      addIncludedModes(tr, builder);
      addTransitFilters(tr, builder);

      return builder.buildRequest();
    }
    throw new IllegalArgumentException("TripRequest must have one origin and one destination.");
  }

  private void addTransitFilters(OJPTripRequestStructure tr, RouteRequestBuilder builder) {
    var includedAgencies = filterMapper.includedAgencies(tr);
    if (!includedAgencies.isEmpty()) {
      builder.withJourney(j ->
        j.withTransit(t ->
          t.withFilter(b -> b.addSelect(SelectRequest.of().withAgencies(includedAgencies).build()))
        )
      );
    }

    var excludedAgencies = filterMapper.excludedAgencies(tr);
    if (!excludedAgencies.isEmpty()) {
      builder.withJourney(j ->
        j.withTransit(t ->
          t.withFilter(b -> b.addNot(SelectRequest.of().withAgencies(excludedAgencies).build()))
        )
      );
    }

    var includedRoutes = filterMapper.includedRoutes(tr);
    if (!includedRoutes.isEmpty()) {
      builder.withJourney(j ->
        j.withTransit(t ->
          t.withFilter(b -> b.addSelect(SelectRequest.of().withRoutes(includedRoutes).build()))
        )
      );
    }

    var excludedRoutes = filterMapper.excludedRoutes(tr);
    if (!excludedRoutes.isEmpty()) {
      builder.withJourney(j ->
        j.withTransit(t ->
          t.withFilter(b -> b.addNot(SelectRequest.of().withRoutes(excludedRoutes).build()))
        )
      );
    }
  }

  private Optional<Duration> transferSlack(OJPTripRequestStructure tr) {
    return Optional.ofNullable(tr.getParams()).map(TripParamStructure::getAdditionalTransferTime);
  }

  private GenericLocation toGenericLocation(PlaceContextStructure place, String name) {
    if (place == null || place.getPlaceRef() == null) {
      throw new IllegalArgumentException("PlaceContext of %s is empty".formatted(name));
    } else if (place.getPlaceRef().getGeoPosition() != null) {
      var g = place.getPlaceRef().getGeoPosition();
      return GenericLocation.fromCoordinate(g.getLatitude(), g.getLongitude());
    } else if (
      Optional.ofNullable(place.getPlaceRef().getStopPlaceRef())
        .map(r -> r.getValue())
        .isPresent()
    ) {
      var id = idMapper.parse(place.getPlaceRef().getStopPlaceRef().getValue());
      return GenericLocation.fromStopId(id);
    } else if (
      Optional.ofNullable(place.getPlaceRef().getStopPointRef())
        .map(r -> r.getValue())
        .isPresent()
    ) {
      var id = idMapper.parse(place.getPlaceRef().getStopPointRef().getValue());
      return GenericLocation.fromStopId(id);
    } else {
      throw new IllegalArgumentException(
        "PlaceContext of %s contains neither stop reference nor coordinates.".formatted(name)
      );
    }
  }

  public static Set<TripResponseMapper.OptionalFeature> optionalFeatures(
    OJPTripRequestStructure tr
  ) {
    var includeIntermediateStops = Optional.ofNullable(tr.getParams())
      .map(TripParamStructure::isIncludeIntermediateStops)
      .filter(b -> !FALSE.equals(b))
      .isPresent();

    if (includeIntermediateStops) {
      return Set.of(INTERMEDIATE_STOPS);
    } else {
      return Set.of();
    }
  }

  private static void addTime(
    PlaceContextStructure origin,
    PlaceContextStructure destination,
    RouteRequestBuilder builder
  ) {
    if (origin.getDepArrTime() != null) {
      // time zone doesn't matter but UTC is the fallback when unspecified
      builder.withDateTime(origin.getDepArrTime().atZone(UTC).toInstant()).withArriveBy(false);
    } else if (destination.getDepArrTime() != null) {
      builder.withDateTime(destination.getDepArrTime().atZone(UTC).toInstant()).withArriveBy(true);
    }
  }

  private static RequestModes extractAccessAndEgressModes(OJPTripRequestStructure tr) {
    var filters = modeFilter(tr)
      .stream()
      .flatMap(Collection::stream)
      .filter(f -> !f.getPersonalMode().isEmpty())
      .toList();

    if (filters.isEmpty()) {
      return RequestModes.defaultRequestModes();
    } else if (filters.stream().anyMatch(f -> !FALSE.equals(f.isExclude()))) {
      throw new IllegalArgumentException(
        "Excluding personal modes is not supported (exclusion is default, set exclude=false)."
      );
    } else if (filters.size() != 1 || filters.getFirst().getPersonalMode().size() != 1) {
      throw new IllegalArgumentException("Can only select a single personal mode.");
    } else {
      var mode = filters.getFirst().getPersonalMode().getFirst();
      var streetMode = PersonalModeMapper.toStreetMode(mode);

      return RequestModes.defaultRequestModes()
        .copyOf()
        .withAccessMode(streetMode)
        .withTransferMode(StreetMode.WALK)
        .withEgressMode(streetMode)
        .withDirectMode(streetMode)
        .build();
    }
  }

  private static boolean ignoreRealtime(OJPTripRequestStructure tr) {
    return Optional.ofNullable(tr.getParams())
      .map(TripParamStructure::getUseRealtimeData)
      .filter(e -> e == UseRealtimeDataEnumeration.NONE)
      .isPresent();
  }

  private static void addIncludedModes(OJPTripRequestStructure tr, RouteRequestBuilder builder) {
    var includedModes = filterModes(tr, f -> FALSE.equals(f.isExclude()));
    if (!includedModes.isEmpty()) {
      builder.withJourney(r ->
        r.withTransit(t ->
          t.withFilter(b ->
            b.addSelect(SelectRequest.of().withTransportModes(includedModes).build())
          )
        )
      );
    }
  }

  private static void addExcludedModes(OJPTripRequestStructure tr, RouteRequestBuilder builder) {
    var excludedModes = filterModes(tr, f -> !FALSE.equals(f.isExclude()));
    if (!excludedModes.isEmpty()) {
      builder.withJourney(r ->
        r.withTransit(t ->
          t.withFilter(b -> b.addNot(SelectRequest.of().withTransportModes(excludedModes).build()))
        )
      );
    }
  }

  private static int numItineraries(OJPTripRequestStructure tr) {
    return Optional.ofNullable(tr.getParams())
      .map(TripParamStructure::getNumberOfResults)
      .orElse(100);
  }

  private static List<MainAndSubMode> filterModes(
    OJPTripRequestStructure tr,
    Predicate<ModeAndModeOfOperationFilterStructure> filterPredicate
  ) {
    return modeFilter(tr)
      .stream()
      .flatMap(List::stream)
      .filter(filterPredicate)
      .flatMap(filter -> filter.getPtMode().stream())
      .map(PtModeMapper::map)
      .map(MainAndSubMode::new)
      .collect(Collectors.toList());
  }

  private static Optional<List<ModeAndModeOfOperationFilterStructure>> modeFilter(
    OJPTripRequestStructure tr
  ) {
    return Optional.ofNullable(tr.getParams()).map(
      TripParamStructure::getModeAndModeOfOperationFilter
    );
  }
}
