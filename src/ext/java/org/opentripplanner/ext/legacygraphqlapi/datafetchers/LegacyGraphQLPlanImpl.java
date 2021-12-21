package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.stream.Collectors;
import org.opentripplanner.api.mapping.PlannerErrorMapper;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PageCursor;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.TripSearchMetadata;

public class LegacyGraphQLPlanImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLPlan {

  @Override
  public DataFetcher<Long> date() {
    return environment -> getSource(environment).getTripPlan().date.getTime();
  }

  @Override
  public DataFetcher<StopArrival> from() {
    return environment -> new StopArrival(getSource(environment).getTripPlan().from, null, null);
  }

  @Override
  public DataFetcher<StopArrival> to() {
    return environment -> new StopArrival(getSource(environment).getTripPlan().to, null, null);
  }

  @Override
  public DataFetcher<Iterable<Itinerary>> itineraries() {
    return environment -> getSource(environment).getTripPlan().itineraries;
  }

  @Override
  public DataFetcher<Iterable<String>> messageEnums() {
    return environment -> getSource(environment)
        .getRoutingErrors()
        .stream()
        .map(routingError -> routingError.code)
        .map(Enum::name)
        .collect(Collectors.toList());
  }

  @Override
  public DataFetcher<Iterable<String>> messageStrings() {
    return environment -> getSource(environment)
        .getRoutingErrors()
        .stream()
        .map(PlannerErrorMapper::mapMessage)
        .map(plannerError -> plannerError.message.get(environment.getLocale()))
        .collect(Collectors.toList());
  }

  @Override
  public DataFetcher<Long> prevDateTime() {
    return environment -> {
      TripSearchMetadata metadata = getSource(environment).getMetadata();
      if ( metadata == null || metadata.prevDateTime == null ) { return null; }
      return metadata.prevDateTime.getEpochSecond() * 1000;
    };
  }

  @Override
  public DataFetcher<Long> nextDateTime() {
    return environment -> {
      TripSearchMetadata metadata = getSource(environment).getMetadata();
      if ( metadata == null || metadata.nextDateTime == null ) { return null; }
      return metadata.nextDateTime.getEpochSecond() * 1000;
    };
  }

  @Override
  public DataFetcher<String> previousPageCursor() {
    return environment -> {
      final PageCursor pageCursor = getSource(environment).getPreviousPageCursor();
      return pageCursor != null ? pageCursor.encode() : null;
    };
  }

  @Override
  public DataFetcher<String> nextPageCursor() {
    return environment -> {
      final PageCursor pageCursor = getSource(environment).getNextPageCursor();
      return pageCursor != null ? pageCursor.encode() : null;
    };
  }

  @Override
  public DataFetcher<Long> searchWindowUsed() {
    return environment -> {
      TripSearchMetadata metadata = getSource(environment).getMetadata();
      if ( metadata == null || metadata.searchWindowUsed == null ) { return null; }
      return metadata.searchWindowUsed.toSeconds();
    };
  }

  @Override
  public DataFetcher<DebugOutput> debugOutput() {
    return environment -> getSource(environment).getDebugTimingAggregator().finishedRendering();
  }

  private RoutingResponse getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
