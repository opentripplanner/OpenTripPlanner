package org.opentripplanner.apis.transmodel.model.timetable;

import graphql.AssertException;
import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.service.TransitService;

/**
 * A DatedServiceJourney GraphQL Type for use in endpoints fetching DatedServiceJourney data
 */
public class DatedServiceJourneyType {

  private static final String NAME = "DatedServiceJourney";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  private final FeedScopedIdMapper idMapper;

  public DatedServiceJourneyType(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  public GraphQLObjectType create(
    GraphQLOutputType serviceJourneyType,
    GraphQLOutputType journeyPatternType,
    GraphQLType estimatedCallType,
    GraphQLType quayType
  ) {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description("A planned journey on a specific day")
      .field(GqlUtil.newTransitIdField(idMapper))
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("operatingDay")
          .description(
            "The date this service runs. The date used is based on the service date as opposed to calendar date."
          )
          .type(TransmodelScalars.DATE_SCALAR)
          .dataFetcher(environment ->
            Optional.of(tripOnServiceDate(environment))
              .map(TripOnServiceDate::getServiceDate)
              .orElse(null)
          )
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serviceJourney")
          .description("The service journey this Dated Service Journey is based on")
          .type(new GraphQLNonNull(serviceJourneyType))
          .dataFetcher(environment -> (tripOnServiceDate(environment).getTrip()))
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("tripAlteration")
          .description("Alterations specified on the Trip in the planned data")
          .type(EnumTypes.SERVICE_ALTERATION)
          .dataFetcher(environment -> tripOnServiceDate(environment).getTripAlteration())
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("replacementFor")
          .description("List of the dated service journeys this dated service journeys replaces")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(REF))))
          .dataFetcher(environment -> tripOnServiceDate(environment).getReplacementFor())
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("journeyPattern")
          .description("JourneyPattern for the dated service journey.")
          .type(journeyPatternType)
          .dataFetcher(DatedServiceJourneyType::tripPattern)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quays")
          .description("Quays visited by the dated service journey.")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(quayType))))
          .argument(
            GraphQLArgument.newArgument()
              .name("first")
              .description("Only fetch the first n quays on the service journey")
              .type(Scalars.GraphQLInt)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("last")
              .description("Only fetch the last n quays on the service journey")
              .type(Scalars.GraphQLInt)
              .build()
          )
          .dataFetcher(environment -> {
            Integer first = environment.getArgument("first");
            Integer last = environment.getArgument("last");
            TripPattern tripPattern = tripPattern(environment);

            if (tripPattern == null) {
              return List.of();
            }

            List<StopLocation> stops = tripPattern.getStops();

            if (first != null && last != null) {
              throw new AssertException("Both first and last can't be defined simultaneously.");
            }

            if ((first != null && first < 0) || (last != null && last < 0)) {
              throw new AssertException("first and last must be positive integers.");
            }

            if (first != null && first < stops.size()) {
              return stops.subList(0, first);
            } else if (last != null && last < stops.size()) {
              return stops.subList(stops.size() - last, stops.size());
            }
            return stops;
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("estimatedCalls")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(estimatedCallType))))
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .description(
            "Returns scheduled passingTimes for this dated service journey, " +
            "updated with real-time-updates (if available). "
          )
          .dataFetcher(environment -> {
            TripOnServiceDate tripOnServiceDate = tripOnServiceDate(environment);
            return GqlUtil.getTransitService(environment)
              .findTripTimesOnDate(tripOnServiceDate.getTrip(), tripOnServiceDate.getServiceDate())
              .orElse(List.of());
          })
          .build()
      )
      .build();
  }

  private static TripPattern tripPattern(DataFetchingEnvironment env) {
    TransitService transitService = GqlUtil.getTransitService(env);
    TripOnServiceDate tripOnServiceDate = tripOnServiceDate(env);
    return transitService.findPattern(
      tripOnServiceDate.getTrip(),
      tripOnServiceDate.getServiceDate()
    );
  }

  private static TripOnServiceDate tripOnServiceDate(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
