package org.opentripplanner.apis.gtfs.mapping;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertCauseType;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertEffectType;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertEntitySelectInput;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertEntityType;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertRouteSelectInput;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertSeverityLevelType;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertStopOrStationSelectInput;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertsFilterInput;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertsFilterSelectInput;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLOffsetDateTimeRangeInput;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLTransitMode;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.transit.api.request.TransitAlertRequest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;
import org.opentripplanner.transit.model.filter.transit.AlertEntityType;
import org.opentripplanner.transit.model.filter.transit.TransitAlertEntitySelectRequest;
import org.opentripplanner.transit.model.filter.transit.TransitAlertRouteSelectRequest;
import org.opentripplanner.transit.model.filter.transit.TransitAlertSelectRequest;
import org.opentripplanner.transit.model.filter.transit.TransitAlertStopOrStationSelectRequest;
import org.opentripplanner.utils.collection.CollectionUtils;

/**
 * Maps the GraphQL {@code alertsConnection} filter input into a {@link TransitAlertRequest}.
 * <p>
 * Each filter is mapped to a {@link FilterRequest} with select/not semantics: an alert matches a
 * filter if it matches at least one of the {@code include} selectors and none of the
 * {@code exclude} selectors. The filters themselves are combined with OR semantics.
 */
public class AlertsConnectionFilterMapper {

  public static TransitAlertRequest map(@Nullable List<GraphQLAlertsFilterInput> filters) {
    if (CollectionUtils.isEmpty(filters)) {
      return TransitAlertRequest.of().build();
    }
    return TransitAlertRequest.of()
      .withFilters(filters.stream().map(AlertsConnectionFilterMapper::toFilterRequest).toList())
      .build();
  }

  private static FilterRequest<TransitAlertSelectRequest> toFilterRequest(
    GraphQLAlertsFilterInput filter
  ) {
    var includes = filter.getGraphQLInclude();
    var excludes = filter.getGraphQLExclude();
    if (includes == null && excludes == null) {
      throw new InvalidInputException(
        "A filter must define at least one of 'filters.include' and 'filters.exclude'."
      );
    }
    CollectionUtils.requireNullOrNonEmpty(includes, "filters.include");
    CollectionUtils.requireNullOrNonEmpty(excludes, "filters.exclude");

    var builder = FilterRequest.<TransitAlertSelectRequest>of();
    if (includes != null) {
      includes
        .stream()
        .map(select -> toSelectRequest(select, "filters.include"))
        .forEach(builder::addSelect);
    }
    if (excludes != null) {
      excludes
        .stream()
        .map(select -> toSelectRequest(select, "filters.exclude"))
        .forEach(builder::addNot);
    }
    return builder.build();
  }

  private static TransitAlertSelectRequest toSelectRequest(
    @Nullable GraphQLAlertsFilterSelectInput select,
    String path
  ) {
    if (select == null) {
      throw new InvalidInputException("'%s' must not contain null values.".formatted(path));
    }
    return TransitAlertSelectRequest.of()
      .withFeeds(requireNullOrNonEmpty(select.getGraphQLFeeds(), path + ".feeds"))
      .withSeverityLevels(severities(select.getGraphQLSeverityLevels(), path + ".severityLevels"))
      .withCauses(causes(select.getGraphQLCauses(), path + ".causes"))
      .withEffects(effects(select.getGraphQLEffects(), path + ".effects"))
      .withTimePeriods(activePeriods(select.getGraphQLActivePeriods(), path + ".activePeriods"))
      .withEntities(entities(select.getGraphQLEntities(), path + ".entities"))
      .build();
  }

  @Nullable
  private static List<TransitAlertEntitySelectRequest> entities(
    @Nullable List<GraphQLAlertEntitySelectInput> values,
    String path
  ) {
    requireNullOrNonEmpty(values, path);
    return values == null
      ? null
      : values
          .stream()
          .map(e -> entity(e, path))
          .toList();
  }

  private static TransitAlertEntitySelectRequest entity(
    GraphQLAlertEntitySelectInput entity,
    String path
  ) {
    var entityTypes = entity.getGraphQLEntityTypes();
    var routes = entity.getGraphQLRoutes();
    var stopsOrStations = entity.getGraphQLStopsOrStations();
    if (entityTypes == null && routes == null && stopsOrStations == null) {
      throw new InvalidInputException(
        "'%s' must define at least one of 'entityTypes', 'routes' and 'stopsOrStations'.".formatted(
          path
        )
      );
    }
    requireNullOrNonEmpty(entityTypes, path + ".entityTypes");
    requireNullOrNonEmpty(routes, path + ".routes");
    requireNullOrNonEmpty(stopsOrStations, path + ".stopsOrStations");
    return TransitAlertEntitySelectRequest.of()
      .withEntityTypes(
        entityTypes == null
          ? null
          : entityTypes.stream().map(AlertsConnectionFilterMapper::entityType).toList()
      )
      .withRoutes(
        routes == null
          ? null
          : routes
              .stream()
              .map(r -> route(r, path + ".routes"))
              .toList()
      )
      .withStopsOrStations(
        stopsOrStations == null
          ? null
          : stopsOrStations
              .stream()
              .map(s -> stopOrStation(s, path + ".stopsOrStations"))
              .toList()
      )
      .build();
  }

  private static TransitAlertRouteSelectRequest route(
    GraphQLAlertRouteSelectInput route,
    String path
  ) {
    requireIdsOrModes(route.getGraphQLIds(), route.getGraphQLModes(), path);
    return TransitAlertRouteSelectRequest.of()
      .withIds(ids(route.getGraphQLIds(), path + ".ids"))
      .withModes(modes(route.getGraphQLModes(), path + ".modes"))
      .build();
  }

  private static TransitAlertStopOrStationSelectRequest stopOrStation(
    GraphQLAlertStopOrStationSelectInput stop,
    String path
  ) {
    requireIdsOrModes(stop.getGraphQLIds(), stop.getGraphQLModes(), path);
    return TransitAlertStopOrStationSelectRequest.of()
      .withIds(ids(stop.getGraphQLIds(), path + ".ids"))
      .withModes(modes(stop.getGraphQLModes(), path + ".modes"))
      .withIncludeParentStationAlerts(
        Boolean.TRUE.equals(stop.getGraphQLIncludeParentStationAlerts())
      )
      .withIncludeChildStopAlerts(Boolean.TRUE.equals(stop.getGraphQLIncludeChildStopAlerts()))
      .build();
  }

  /**
   * Exactly one of the ids and the modes must be set, they can't be combined.
   */
  private static void requireIdsOrModes(
    @Nullable List<String> ids,
    @Nullable List<GraphQLTransitMode> modes,
    String path
  ) {
    if ((ids == null) == (modes == null)) {
      throw new InvalidInputException(
        "'%s' must define exactly one of 'ids' and 'modes'.".formatted(path)
      );
    }
  }

  @Nullable
  private static List<FeedScopedId> ids(@Nullable List<String> values, String path) {
    requireNullOrNonEmpty(values, path);
    if (values == null) {
      return null;
    }
    return values
      .stream()
      .map(id ->
        FeedScopedId.parseOptional(id).orElseThrow(() ->
          new InvalidInputException("'%s' contains an invalid id: '%s'.".formatted(path, id))
        )
      )
      .toList();
  }

  @Nullable
  private static List<TransitMode> modes(@Nullable List<GraphQLTransitMode> values, String path) {
    requireNullOrNonEmpty(values, path);
    return values == null ? null : values.stream().map(TransitModeMapper::map).toList();
  }

  private static AlertEntityType entityType(GraphQLAlertEntityType type) {
    return switch (type) {
      case AGENCY -> AlertEntityType.AGENCY;
      case PATTERN -> AlertEntityType.PATTERN;
      case ROUTE -> AlertEntityType.ROUTE;
      case ROUTE_TYPE -> AlertEntityType.ROUTE_TYPE;
      case STOP -> AlertEntityType.STOP;
      case STOP_ON_ROUTE -> AlertEntityType.STOP_ON_ROUTE;
      case STOP_ON_TRIP -> AlertEntityType.STOP_ON_TRIP;
      case TRIP -> AlertEntityType.TRIP;
      case UNKNOWN -> AlertEntityType.UNKNOWN;
    };
  }

  @Nullable
  private static List<AlertSeverity> severities(
    @Nullable List<GraphQLAlertSeverityLevelType> values,
    String path
  ) {
    requireNullOrNonEmpty(values, path);
    return values == null
      ? null
      : values
          .stream()
          .flatMap(s -> SeverityMapper.getAlertSeverities(s).stream())
          .toList();
  }

  @Nullable
  private static List<AlertCause> causes(
    @Nullable List<GraphQLAlertCauseType> values,
    String path
  ) {
    requireNullOrNonEmpty(values, path);
    return values == null ? null : values.stream().map(AlertCauseMapper::getAlertCause).toList();
  }

  @Nullable
  private static List<AlertEffect> effects(
    @Nullable List<GraphQLAlertEffectType> values,
    String path
  ) {
    requireNullOrNonEmpty(values, path);
    return values == null ? null : values.stream().map(AlertEffectMapper::getAlertEffect).toList();
  }

  @Nullable
  private static List<TimePeriod> activePeriods(
    @Nullable List<GraphQLOffsetDateTimeRangeInput> ranges,
    String path
  ) {
    requireNullOrNonEmpty(ranges, path);
    return ranges == null
      ? null
      : ranges.stream().map(AlertsConnectionFilterMapper::activePeriod).toList();
  }

  private static TimePeriod activePeriod(GraphQLOffsetDateTimeRangeInput range) {
    var start = range.getGraphQLStart() != null ? range.getGraphQLStart().toInstant() : null;
    var end = range.getGraphQLEnd() != null ? range.getGraphQLEnd().toInstant() : null;
    return TimePeriod.of(start, end);
  }

  /**
   * A dimension is either unset or has at least one non-null value. An empty list would filter away
   * everything, which is never what the caller wants, so it is rejected.
   */
  @Nullable
  private static <T> List<T> requireNullOrNonEmpty(@Nullable List<T> values, String path) {
    if (values == null) {
      return null;
    }
    if (values.isEmpty()) {
      throw new InvalidInputException(
        "'%s' must be either null or have at least one entry.".formatted(path)
      );
    }
    if (values.stream().anyMatch(Objects::isNull)) {
      throw new InvalidInputException("'%s' must not contain null values.".formatted(path));
    }
    return values;
  }
}
