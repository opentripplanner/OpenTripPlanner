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
import org.opentripplanner.apis.transmodel.mapping.TransitIdMapper;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.api.request.TripOnServiceDateRequest;
import org.opentripplanner.transit.api.request.TripOnServiceDateRequestBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;

/**
 * A GraphQL query for retrieving data on DatedServiceJourneys
 */
public class DatedServiceJourneyQuery {

  public static GraphQLFieldDefinition createGetById(GraphQLOutputType datedServiceJourneyType) {
    return GraphQLFieldDefinition.newFieldDefinition()
      .name("datedServiceJourney")
      .type(datedServiceJourneyType)
      .description("Get a single dated service journey based on its id")
      .argument(GraphQLArgument.newArgument().name("id").type(Scalars.GraphQLString))
      .dataFetcher(environment -> {
        FeedScopedId id = TransitIdMapper.mapIDToDomain(environment.getArgument("id"));

        return GqlUtil.getTransitService(environment).getTripOnServiceDate(id);
      })
      .build();
  }

  public static GraphQLFieldDefinition createQuery(GraphQLOutputType datedServiceJourneyType) {
    return GraphQLFieldDefinition.newFieldDefinition()
      .name("datedServiceJourneys")
      .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(datedServiceJourneyType))))
      .description("Get all dated service journeys, matching the filters")
      .argument(
        GraphQLArgument.newArgument()
          .name("lines")
          .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("serviceJourneys")
          .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("privateCodes")
          .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("operatingDays")
          .type(
            new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(TransmodelScalars.DATE_SCALAR)))
          )
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("alterations")
          .type(new GraphQLList(new GraphQLNonNull(EnumTypes.SERVICE_ALTERATION)))
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("authorities")
          .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("replacementFor")
          .description(
            "Get all DatedServiceJourneys, which are replacing any of the given DatedServiceJourneys ids"
          )
          .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
      )
      .dataFetcher(environment -> {
        // The null safety checks are not needed here - they are taken care of by the request
        // object, but let's use the mapping method and leave this improvement until all APIs
        // are pushing this check into the domain request.
        var authorities = FilterValues.ofEmptyIsEverything(
          "authorities",
          mapIDsToDomainNullSafe(environment.getArgument("authorities"))
        );
        var lines = FilterValues.ofEmptyIsEverything(
          "lines",
          mapIDsToDomainNullSafe(environment.getArgument("lines"))
        );
        var serviceJourneys = FilterValues.ofEmptyIsEverything(
          "serviceJourneys",
          mapIDsToDomainNullSafe(environment.getArgument("serviceJourneys"))
        );
        var replacementFor = FilterValues.ofEmptyIsEverything(
          "replacementFor",
          mapIDsToDomainNullSafe(environment.getArgument("replacementFor"))
        );
        var privateCodes = FilterValues.ofEmptyIsEverything(
          "privateCodes",
          environment.<List<String>>getArgument("privateCodes")
        );
        var operatingDays = FilterValues.ofRequired(
          "operatingDays",
          environment.<List<LocalDate>>getArgument("operatingDays")
        );
        var alterations = FilterValues.ofEmptyIsEverything(
          "alterations",
          environment.<List<TripAlteration>>getArgument("alterations")
        );

        TripOnServiceDateRequestBuilder tripOnServiceDateRequestBuilder =
          TripOnServiceDateRequest.of(operatingDays)
            .withAgencies(authorities)
            .withRoutes(lines)
            .withServiceJourneys(serviceJourneys)
            .withReplacementFor(replacementFor);

        tripOnServiceDateRequestBuilder =
          tripOnServiceDateRequestBuilder.withNetexInternalPlanningCodes(privateCodes);

        tripOnServiceDateRequestBuilder = tripOnServiceDateRequestBuilder.withAlterations(
          alterations
        );

        return GqlUtil.getTransitService(environment).findTripsOnServiceDate(
          tripOnServiceDateRequestBuilder.build()
        );
      })
      .build();
  }
}
