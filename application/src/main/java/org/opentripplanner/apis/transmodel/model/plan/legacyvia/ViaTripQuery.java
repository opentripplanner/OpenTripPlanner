package org.opentripplanner.apis.transmodel.model.plan.legacyvia;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLPlanner;
import org.opentripplanner.apis.transmodel.model.DefaultRouteRequestType;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.framework.LocationInputType;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;

public class ViaTripQuery {

  public static GraphQLFieldDefinition create(
    DefaultRouteRequestType routing,
    GraphQLOutputType viaTripType,
    GraphQLInputObjectType viaLocationInputType,
    GraphQLInputObjectType viaSegmentInputType,
    GraphQLScalarType dateTimeScalar
  ) {
    return GraphQLFieldDefinition.newFieldDefinition()
      .name("viaTrip")
      .description(
        "Via trip search. Find trip patterns traveling via one or more intermediate (via) locations."
      )
      .deprecate("Use the regular trip query with via stop instead.")
      .type(new GraphQLNonNull(viaTripType))
      .withDirective(TransmodelDirectives.TIMING_DATA)
      .argument(
        GraphQLArgument.newArgument()
          .name("dateTime")
          .description(
            "Date and time for the earliest time the user is willing to start the journey " +
            "(if arriveBy=false/not set) or the latest acceptable time of arriving " +
            "(arriveBy=true). Defaults to now"
          )
          .type(dateTimeScalar)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("searchWindow")
          .description(
            """
            The length of the search-window. This parameter is optional.

            The search-window is defined as the duration between the earliest-departure-time (EDT)
            and the latest-departure-time (LDT). OTP will search for all itineraries in this
            departure window. If `arriveBy=true` the `dateTime` parameter is the
            latest-arrival-time, so OTP will dynamically calculate the EDT. Using a short
            search-window is faster than using a longer one, but the search duration is not linear.
            Using a \"too\" short search-window will waste resources server side, while using a
            search-window that is too long will be slow.

            OTP will dynamically calculate a reasonable value for the search-window, if not
            provided. The calculation comes with a significant overhead (10-20% extra). Whether you
            should use the dynamic calculated value or pass in a value depends on your use-case.
            For a travel planner in a small geographical area, with a dense network of public
            transportation, a fixed value between 40 minutes and 2 hours makes sense. To find the
            appropriate search-window, adjust it so that the number of itineraries on average is
            around the wanted `numItineraries`. Make sure you set the `numItineraries` to a high
            number while testing. For a country wide area like Norway, using the dynamic
            search-window is the best.

            When paginating, the search-window is calculated using the `numItineraries` in the
            original search together with statistics from the search for the last page. This
            behaviour is configured server side, and can not be overridden from the client.

            The search-window used is returned to the response metadata as `searchWindowUsed` for
            debugging purposes.
            """
          )
          .type(new GraphQLNonNull(TransmodelScalars.DURATION_SCALAR))
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("pageCursor")
          .description(
            "Use the cursor to go to the next \"page\" of itineraries. Copy the cursor from " +
            "the last response and keep the original request as is. This will enable you to " +
            "search for itineraries in the next or previous time-window."
          )
          .type(Scalars.GraphQLString)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("from")
          .description("The start location")
          .type(new GraphQLNonNull(LocationInputType.INPUT_TYPE))
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("to")
          .description("The destination location")
          .type(new GraphQLNonNull(LocationInputType.INPUT_TYPE))
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("via")
          .description("The locations needed to be visited along the route.")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(viaLocationInputType))))
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("segments")
          .description(
            "The requests for the individual segments of the search. The first segment is from " +
            "the start location (`from`) to the first entry in the `via` locations list and the " +
            "last is from the last entry in the `via` locations list to the end location (`to`). " +
            "Note that the list must have length of exactly one greater than the `via` field."
          )
          .type(new GraphQLList(new GraphQLNonNull(viaSegmentInputType)))
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("numTripPatterns")
          .description(
            "The maximum number of trip patterns segment to return. Note! This reduces the number " +
            "of trip patterns AFTER the OTP travel search is done in a post-filtering process. " +
            "There is little/no performance gain in reducing the number of trip patterns returned. " +
            "See also the trip meta-data on how to implement paging."
          )
          .defaultValueProgrammatic(routing.request.numItineraries())
          .type(Scalars.GraphQLInt)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("wheelchairAccessible")
          .description(
            "Whether the trip must be wheelchair accessible. Supported for the street part to " +
            "the search, not implemented for the transit yet."
          )
          .type(Scalars.GraphQLBoolean)
          .defaultValueProgrammatic(routing.request.wheelchair())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("locale")
          .description(
            "The preferable language to use for text targeted the end user. Note! The data " +
            "quality is limited, only stop and quay names are translates, and not in all " +
            "places of the API."
          )
          .type(EnumTypes.LOCALE)
          .defaultValueProgrammatic("no")
          .build()
      )
      .dataFetcher(environment -> new TransmodelGraphQLPlanner().planVia(environment))
      .build();
  }
}
