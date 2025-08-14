package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.apis.gtfs.mapping.AlertEffectMapper.getGraphQLEffect;
import static org.opentripplanner.apis.gtfs.mapping.SeverityMapper.getGraphQLSeverity;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertEffectType;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertSeverityLevelType;
import org.opentripplanner.apis.gtfs.mapping.AlertCauseMapper;
import org.opentripplanner.apis.gtfs.model.RouteTypeModel;
import org.opentripplanner.apis.gtfs.model.StopOnRouteModel;
import org.opentripplanner.apis.gtfs.model.StopOnTripModel;
import org.opentripplanner.apis.gtfs.model.UnknownModel;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.EntitySelector.DirectionAndRoute;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;

public class AlertImpl implements GraphQLDataFetchers.GraphQLAlert {

  private static final String FALLBACK_EMPTY_STRING = "";

  @Override
  public DataFetcher<Agency> agency() {
    return environment ->
      getSource(environment)
        .entities()
        .stream()
        .filter(EntitySelector.Agency.class::isInstance)
        .findAny()
        .map(EntitySelector.Agency.class::cast)
        .map(entitySelector -> getTransitService(environment).getAgency(entitySelector.agencyId()))
        .orElse(null);
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLAlertCauseType> alertCause() {
    return environment -> AlertCauseMapper.getGraphQLCause(getSource(environment).cause());
  }

  @Override
  public DataFetcher<String> alertDescriptionText() {
    return environment -> {
      var alert = getSource(environment);
      var descriptionText = GraphQLUtils.getTranslation(
        alert.descriptionText().or(alert::headerText).orElse(null),
        environment
      );
      return descriptionText != null ? descriptionText : FALLBACK_EMPTY_STRING;
    };
  }

  @Override
  public DataFetcher<Iterable<Map.Entry<String, String>>> alertDescriptionTextTranslations() {
    return environment ->
      getSource(environment).descriptionText().map(this::getTranslations).orElse(List.of());
  }

  @Override
  public DataFetcher<GraphQLAlertEffectType> alertEffect() {
    return environment -> getGraphQLEffect(getSource(environment).effect());
  }

  @Override
  public DataFetcher<Integer> alertHash() {
    return environment -> {
      TransitAlert alert = getSource(environment);
      return Objects.hash(
        alert.descriptionText(),
        alert.headerText(),
        alert.url(),
        alert.cause(),
        alert.effect(),
        alert.severity()
      );
    };
  }

  @Override
  public DataFetcher<String> alertHeaderText() {
    return environment -> {
      var alert = getSource(environment);
      var headerText = GraphQLUtils.getTranslation(
        alert.headerText().or(alert::descriptionText).orElse(null),
        environment
      );
      return headerText != null ? headerText : FALLBACK_EMPTY_STRING;
    };
  }

  @Override
  public DataFetcher<Iterable<Map.Entry<String, String>>> alertHeaderTextTranslations() {
    return environment ->
      getSource(environment).headerText().map(this::getTranslations).orElse(List.of());
  }

  @Override
  public DataFetcher<GraphQLAlertSeverityLevelType> alertSeverityLevel() {
    return environment -> getGraphQLSeverity(getSource(environment).severity());
  }

  @Override
  public DataFetcher<String> alertUrl() {
    return environment ->
      GraphQLUtils.getTranslation(getSource(environment).url().orElse(null), environment);
  }

  @Override
  public DataFetcher<Iterable<Map.Entry<String, String>>> alertUrlTranslations() {
    return environment -> getSource(environment).url().map(this::getTranslations).orElse(List.of());
  }

  @Override
  public DataFetcher<Long> effectiveEndDate() {
    return environment -> {
      Instant effectiveEndDate = getSource(environment).getEffectiveEndDate();
      if (effectiveEndDate == null) {
        return null;
      }
      return effectiveEndDate.getEpochSecond();
    };
  }

  @Override
  public DataFetcher<Long> effectiveStartDate() {
    return environment -> {
      Instant effectiveStartDate = getSource(environment).getEffectiveStartDate();
      if (effectiveStartDate == null) {
        return null;
      }
      return effectiveStartDate.getEpochSecond();
    };
  }

  @Nullable
  private Object getStopOrStation(TransitService transitService, FeedScopedId id) {
    StopLocation stop = transitService.getStopLocation(id);
    StopLocationsGroup station = transitService.getStopLocationsGroup(id);
    return stop == null ? station : stop;
  }

  @Override
  public DataFetcher<Iterable<Object>> entities() {
    return environment ->
      getSource(environment)
        .entities()
        .stream()
        .map(entitySelector -> {
          if (entitySelector instanceof EntitySelector.Stop) {
            FeedScopedId id = ((EntitySelector.Stop) entitySelector).stopId();
            Object stop = getStopOrStation(getTransitService(environment), id);
            return List.of(getAlertEntityOrUnknown(stop, id.toString(), "stop"));
          }
          if (entitySelector instanceof EntitySelector.Agency) {
            FeedScopedId id = ((EntitySelector.Agency) entitySelector).agencyId();
            Agency agency = getTransitService(environment).getAgency(id);
            return List.of(getAlertEntityOrUnknown(agency, id.toString(), "agency"));
          }
          if (entitySelector instanceof EntitySelector.Route) {
            FeedScopedId id = ((EntitySelector.Route) entitySelector).routeId();
            Route route = getTransitService(environment).getRoute(id);
            return List.of(getAlertEntityOrUnknown(route, id.toString(), "route"));
          }
          if (entitySelector instanceof EntitySelector.Trip) {
            FeedScopedId id = ((EntitySelector.Trip) entitySelector).tripId();
            Trip trip = getTransitService(environment).getTrip(id);
            return List.of(getAlertEntityOrUnknown(trip, id.toString(), "trip"));
          }
          if (entitySelector instanceof EntitySelector.StopAndRoute stopAndRoute) {
            FeedScopedId stopId = stopAndRoute.stopId();
            FeedScopedId routeId = stopAndRoute.routeId();
            StopLocation stop = getTransitService(environment).getRegularStop(stopId);
            Route route = getTransitService(environment).getRoute(routeId);
            return List.of(
              stop != null && route != null
                ? new StopOnRouteModel(stop, route)
                : getUnknownForAlertEntityPair(
                  stop,
                  route,
                  stopId.toString(),
                  routeId.toString(),
                  "stop",
                  "route"
                )
            );
          }
          if (entitySelector instanceof EntitySelector.StopAndTrip stopAndTrip) {
            FeedScopedId stopId = stopAndTrip.stopId();
            FeedScopedId tripId = stopAndTrip.tripId();
            StopLocation stop = getTransitService(environment).getRegularStop(stopId);
            Trip trip = getTransitService(environment).getTrip(tripId);
            return List.of(
              stop != null && trip != null
                ? new StopOnTripModel(stop, trip)
                : getUnknownForAlertEntityPair(
                  stop,
                  trip,
                  stopId.toString(),
                  tripId.toString(),
                  "stop",
                  "trip"
                )
            );
          }
          if (entitySelector instanceof EntitySelector.RouteTypeAndAgency) {
            FeedScopedId agencyId = ((EntitySelector.RouteTypeAndAgency) entitySelector).agencyId();
            int routeType = ((EntitySelector.RouteTypeAndAgency) entitySelector).routeType();
            Agency agency = getTransitService(environment).getAgency(agencyId);
            return List.of(
              agency != null
                ? new RouteTypeModel(agency, routeType, agency.getId().getFeedId())
                : getUnknownForAlertEntityPair(
                  agency,
                  routeType,
                  null,
                  Integer.toString(routeType),
                  "agency",
                  "route type"
                )
            );
          }
          if (entitySelector instanceof EntitySelector.RouteType) {
            int routeType = ((EntitySelector.RouteType) entitySelector).routeType();
            String feedId = ((EntitySelector.RouteType) entitySelector).feedId();
            return List.of(new RouteTypeModel(null, routeType, feedId));
          }
          if (entitySelector instanceof EntitySelector.DirectionAndRoute) {
            Direction direction = ((DirectionAndRoute) entitySelector).direction();
            FeedScopedId routeId = ((EntitySelector.DirectionAndRoute) entitySelector).routeId();
            Route route = getTransitService(environment).getRoute(routeId);
            return route != null
              ? getTransitService(environment)
                .findPatterns(route)
                .stream()
                .filter(pattern -> pattern.getDirection() == direction)
                .collect(Collectors.toList())
              : List.of(
                getUnknownForAlertEntityPair(
                  route,
                  direction,
                  null,
                  direction.name(),
                  "route",
                  "direction"
                )
              );
          }
          if (entitySelector instanceof EntitySelector.Unknown) {
            final List<Object> objects = List.of(
              new UnknownModel(((EntitySelector.Unknown) entitySelector).description())
            );
            return objects;
          }
          return List.of();
        })
        .flatMap(Collection::stream)
        .map(Object.class::cast)
        .collect(Collectors.toList());
  }

  @Override
  public DataFetcher<String> feed() {
    return environment -> getSource(environment).getId().getFeedId();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId("Alert", getSource(environment).getId().toString());
  }

  // This is deprecated
  @Override
  public DataFetcher<Iterable<TripPattern>> patterns() {
    return environment -> Collections.emptyList();
  }

  @Override
  public DataFetcher<Route> route() {
    return environment ->
      getSource(environment)
        .entities()
        .stream()
        .filter(entitySelector -> entitySelector instanceof EntitySelector.Route)
        .findAny()
        .map(EntitySelector.Route.class::cast)
        .map(entitySelector -> getTransitService(environment).getRoute(entitySelector.routeId()))
        .orElse(null);
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment ->
      getSource(environment)
        .entities()
        .stream()
        .filter(entitySelector -> entitySelector instanceof EntitySelector.Stop)
        .findAny()
        .map(EntitySelector.Stop.class::cast)
        .map(entitySelector ->
          getTransitService(environment).getRegularStop(entitySelector.stopId())
        )
        .orElse(null);
  }

  @Override
  public DataFetcher<Trip> trip() {
    return environment ->
      getSource(environment)
        .entities()
        .stream()
        .filter(entitySelector -> entitySelector instanceof EntitySelector.Trip)
        .findAny()
        .map(EntitySelector.Trip.class::cast)
        .map(entitySelector -> getTransitService(environment).getTrip(entitySelector.tripId()))
        .orElse(null);
  }

  private Object getAlertEntityOrUnknown(@Nullable Object entity, String id, String type) {
    if (entity != null) {
      return entity;
    }
    return new UnknownModel(
      String.format(
        "Alert's entity selector was %s with id %s but the %s doesn't exist.",
        type,
        id,
        type
      )
    );
  }

  private Object getUnknownForAlertEntityPair(
    @Nullable Object entityA,
    @Nullable Object entityB,
    String idA,
    String idB,
    String typeA,
    String typeB
  ) {
    if (entityA == null && entityB == null) {
      return new UnknownModel(
        String.format(
          "Alert's entity selector was %s with id %s and %s with id %s but the %s and %s don't exist.",
          typeA,
          idA,
          typeB,
          idB,
          typeA,
          typeB
        )
      );
    }
    if (entityA == null) {
      return new UnknownModel(
        String.format(
          "Alert's entity selector was %s with id %s and %s with id %s but the %s doesn't exist.",
          typeA,
          idA,
          typeB,
          idB,
          typeA
        )
      );
    }
    return new UnknownModel(
      String.format(
        "Alert's entity selector was %s with id %s and %s with id %s but the %s doesn't exist.",
        typeA,
        idA,
        typeB,
        idB,
        typeB
      )
    );
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private TransitAlert getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }

  private Iterable<Map.Entry<String, String>> getTranslations(I18NString text) {
    return text instanceof TranslatedString
      ? ((TranslatedString) text).getTranslations()
      : Collections.emptyList();
  }
}
