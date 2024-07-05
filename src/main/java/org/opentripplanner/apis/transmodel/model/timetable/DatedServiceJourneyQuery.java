package org.opentripplanner.apis.transmodel.model.timetable;

import static org.opentripplanner.apis.transmodel.mapping.TransitIdMapper.mapIDsToDomainNullSafe;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.apis.transmodel.mapping.TransitIdMapper;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * A GraphQL query for retrieving data on DatedServiceJourneys
 */
public class DatedServiceJourneyQuery {

  public static GraphQLFieldDefinition createGetById(GraphQLOutputType datedServiceJourneyType) {
    return GraphQLFieldDefinition
      .newFieldDefinition()
      .name("datedServiceJourney")
      .type(datedServiceJourneyType)
      .description("Get a single dated service journey based on its id")
      .argument(GraphQLArgument.newArgument().name("id").type(Scalars.GraphQLString))
      .dataFetcher(environment -> {
        FeedScopedId id = TransitIdMapper.mapIDToDomain(environment.getArgument("id"));

        return GqlUtil.getTransitService(environment).getTripOnServiceDateById(id);
      })
      .build();
  }

  public static GraphQLFieldDefinition createQuery(
    GraphQLOutputType datedServiceJourneyType,
    GqlUtil gqlUtil
  ) {
    return GraphQLFieldDefinition
      .newFieldDefinition()
      .name("datedServiceJourneys")
      .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(datedServiceJourneyType))))
      .description("Get all dated service journeys, matching the filters")
      .argument(
        GraphQLArgument
          .newArgument()
          .name("lines")
          .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
      )
      .argument(
        GraphQLArgument
          .newArgument()
          .name("serviceJourneys")
          .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
      )
      .argument(
        GraphQLArgument
          .newArgument()
          .name("privateCodes")
          .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
      )
      .argument(
        GraphQLArgument
          .newArgument()
          .name("operatingDays")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(gqlUtil.dateScalar))))
      )
      .argument(
        GraphQLArgument
          .newArgument()
          .name("alterations")
          .type(new GraphQLList(new GraphQLNonNull(EnumTypes.SERVICE_ALTERATION)))
      )
      .argument(
        GraphQLArgument
          .newArgument()
          .name("authorities")
          .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
      )
      .argument(
        GraphQLArgument
          .newArgument()
          .name("replacementFor")
          .description(
            "Get all DatedServiceJourneys, which are replacing any of the given DatedServiceJourneys ids"
          )
          .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
      )
      .dataFetcher(environment -> {
        Stream<TripOnServiceDate> stream = GqlUtil
          .getTransitService(environment)
          .getAllTripOnServiceDates()
          .stream();

        var lines = mapIDsToDomainNullSafe(environment.getArgument("lines"));
        var serviceJourneys = mapIDsToDomainNullSafe(environment.getArgument("serviceJourneys"));
        var privateCodes = environment.<List<String>>getArgument("privateCodes");
        var operatingDays = environment.<List<LocalDate>>getArgument("operatingDays");
        var alterations = environment.<List<TripAlteration>>getArgument("alterations");
        var authorities = mapIDsToDomainNullSafe(environment.getArgument("authorities"));
        var replacementFor = mapIDsToDomainNullSafe(environment.getArgument("replacementFor"));

        if (!lines.isEmpty()) {
          stream =
            stream.filter(tripOnServiceDate ->
              lines.contains(tripOnServiceDate.getTrip().getRoute().getId())
            );
        }

        if (!serviceJourneys.isEmpty()) {
          stream =
            stream.filter(tripOnServiceDate ->
              serviceJourneys.contains(tripOnServiceDate.getTrip().getId())
            );
        }

        if (privateCodes != null && !privateCodes.isEmpty()) {
          stream =
            stream.filter(tripOnServiceDate ->
              privateCodes.contains(tripOnServiceDate.getTrip().getNetexInternalPlanningCode())
            );
        }

        // At least one operationg day is required
        var days = operatingDays.stream().toList();

        stream =
          stream.filter(tripOnServiceDate -> days.contains(tripOnServiceDate.getServiceDate()));

        if (alterations != null && !alterations.isEmpty()) {
          stream =
            stream.filter(tripOnServiceDate ->
              alterations.contains(tripOnServiceDate.getTripAlteration())
            );
        }

        if (!authorities.isEmpty()) {
          stream =
            stream.filter(tripOnServiceDate ->
              authorities.contains(tripOnServiceDate.getTrip().getRoute().getAgency().getId())
            );
        }

        if (!replacementFor.isEmpty()) {
          stream =
            stream.filter(tripOnServiceDate ->
              !tripOnServiceDate.getReplacementFor().isEmpty() &&
              tripOnServiceDate
                .getReplacementFor()
                .stream()
                .anyMatch(replacement -> replacementFor.contains(replacement.getId()))
            );
        }

        return stream.toList();
      })
      .build();
  }
}
