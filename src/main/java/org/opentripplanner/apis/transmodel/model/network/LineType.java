package org.opentripplanner.apis.transmodel.model.network;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import java.util.Collection;
import java.util.stream.Collectors;
import org.opentripplanner.apis.transmodel.mapping.TransitIdMapper;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;

public class LineType {

  private static final String NAME = "Line";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create(
    GraphQLOutputType bookingArrangementType,
    GraphQLOutputType authorityType,
    GraphQLOutputType operatorType,
    GraphQLOutputType noticeType,
    GraphQLOutputType quayType,
    GraphQLObjectType presentationType,
    GraphQLOutputType journeyPatternType,
    GraphQLOutputType serviceJourneyType,
    GraphQLOutputType ptSituationElementType,
    GraphQLOutputType brandingType,
    GraphQLOutputType groupOfLinesType
  ) {
    return GraphQLObjectType
      .newObject()
      .name(NAME)
      .description(
        "A group of routes which is generally known to the public by a similar name or number"
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("id")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .dataFetcher(environment -> TransitIdMapper.mapEntityIDToApi(environment.getSource()))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("authority")
          .type(authorityType)
          .dataFetcher(environment -> (((Route) environment.getSource()).getAgency()))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("operator")
          .type(operatorType)
          .dataFetcher(environment -> (((Route) environment.getSource()).getOperator()))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("branding")
          .type(brandingType)
          .dataFetcher(environment -> ((Route) environment.getSource()).getBranding())
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("publicCode")
          .type(Scalars.GraphQLString)
          .description(
            "Publicly announced code for line, differentiating it from other lines for the same operator."
          )
          .dataFetcher(environment -> (((Route) environment.getSource()).getShortName()))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("name")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((Route) environment.getSource()).getLongName())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("transportMode")
          .type(EnumTypes.TRANSPORT_MODE)
          .dataFetcher(environment -> ((Route) environment.getSource()).getMode())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("transportSubmode")
          .type(EnumTypes.TRANSPORT_SUBMODE)
          .dataFetcher(environment ->
            TransmodelTransportSubmode.fromValue(
              ((Route) environment.getSource()).getNetexSubmode()
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("description")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((Route) environment.getSource()).getDescription())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition().name("url").type(Scalars.GraphQLString).build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("presentation")
          .type(presentationType)
          .dataFetcher(DataFetchingEnvironment::getSource)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("bikesAllowed")
          .type(EnumTypes.BIKES_ALLOWED)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("journeyPatterns")
          .type(new GraphQLList(journeyPatternType))
          .dataFetcher(environment ->
            GqlUtil.getTransitService(environment).getPatternsForRoute(environment.getSource())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("quays")
          .type(new GraphQLNonNull(new GraphQLList(quayType)))
          .dataFetcher(environment ->
            GqlUtil
              .getTransitService(environment)
              .getPatternsForRoute(environment.getSource())
              .stream()
              .map(TripPattern::getStops)
              .flatMap(Collection::stream)
              .distinct()
              .collect(Collectors.toList())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("serviceJourneys")
          .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
          .dataFetcher(environment ->
            GqlUtil
              .getTransitService(environment)
              .getPatternsForRoute(environment.getSource())
              .stream()
              .flatMap(TripPattern::scheduledTripsAsStream)
              .distinct()
              .collect(Collectors.toList())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("notices")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(noticeType))))
          .dataFetcher(environment -> {
            Route route = environment.getSource();
            return GqlUtil.getTransitService(environment).getNoticesByEntity(route);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("situations")
          .description("Get all situations active for the line.")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
          .dataFetcher(environment ->
            GqlUtil
              .getTransitService(environment)
              .getTransitAlertService()
              .getRouteAlerts(((Route) environment.getSource()).getId())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("flexibleLineType")
          .description("Type of flexible line, or null if line is not flexible.")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((Route) environment.getSource()).getFlexibleLineType())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("bookingArrangements")
          .description("Booking arrangements for flexible line.")
          .type(bookingArrangementType)
          .deprecate(
            "BookingArrangements are defined per stop, and can be found under `passingTimes` or `estimatedCalls`"
          )
          .dataFetcher(environment -> null)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("groupOfLines")
          .description("Groups of lines that line is a part of.")
          .type(new GraphQLNonNull(new GraphQLList(groupOfLinesType)))
          .dataFetcher(environment -> ((Route) environment.getSource()).getGroupsOfRoutes())
          .build()
      )
      .build();
  }
}
