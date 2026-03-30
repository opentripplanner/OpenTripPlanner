package org.opentripplanner.apis.gtfs.mapping;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertCauseType;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertEffectType;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertSeverityLevelType;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertsFilterInput;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertsFilterSelectInput;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLOffsetDateTimeRangeInput;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.transit.api.request.TransitAlertRequest;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;
import org.opentripplanner.transit.model.filter.transit.TransitAlertSelectRequest;
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
      .withSeverityLevels(severities(select.getGraphQLSeverityLevels(), path))
      .withCauses(causes(select.getGraphQLCauses(), path))
      .withEffects(effects(select.getGraphQLEffects(), path))
      .withTimePeriods(activePeriods(select.getGraphQLActivePeriods(), path))
      .build();
  }

  @Nullable
  private static List<AlertSeverity> severities(
    @Nullable List<GraphQLAlertSeverityLevelType> values,
    String path
  ) {
    var checked = requireNullOrNonEmpty(values, path + ".severityLevels");
    return checked == null
      ? null
      : checked
          .stream()
          .flatMap(s -> SeverityMapper.getAlertSeverities(s).stream())
          .toList();
  }

  @Nullable
  private static List<AlertCause> causes(
    @Nullable List<GraphQLAlertCauseType> values,
    String path
  ) {
    var checked = requireNullOrNonEmpty(values, path + ".causes");
    return checked == null ? null : checked.stream().map(AlertCauseMapper::getAlertCause).toList();
  }

  @Nullable
  private static List<AlertEffect> effects(
    @Nullable List<GraphQLAlertEffectType> values,
    String path
  ) {
    var checked = requireNullOrNonEmpty(values, path + ".effects");
    return checked == null
      ? null
      : checked.stream().map(AlertEffectMapper::getAlertEffect).toList();
  }

  @Nullable
  private static List<TimePeriod> activePeriods(
    @Nullable List<GraphQLOffsetDateTimeRangeInput> ranges,
    String path
  ) {
    var checked = requireNullOrNonEmpty(ranges, path + ".activePeriods");
    return checked == null
      ? null
      : checked.stream().map(AlertsConnectionFilterMapper::activePeriod).toList();
  }

  private static TimePeriod activePeriod(GraphQLOffsetDateTimeRangeInput range) {
    var start = range.getGraphQLStart() != null ? range.getGraphQLStart().toInstant() : null;
    var end = range.getGraphQLEnd() != null ? range.getGraphQLEnd().toInstant() : null;
    return TimePeriod.of(start, end);
  }

  /**
   * A dimension is either unset or has at least one non-null value. An empty list would filter
   * away everything, which is never what the caller wants, so it is rejected.
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
    if (values.contains(null)) {
      throw new InvalidInputException("'%s' must not contain null values.".formatted(path));
    }
    return values;
  }
}
