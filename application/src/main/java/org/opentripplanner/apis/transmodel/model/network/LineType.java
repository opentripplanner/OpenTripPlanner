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
import java.util.Optional;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;

public class LineType {

  private static final String NAME = "Line";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  private final FeedScopedIdMapper idMapper;

  public LineType(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  public GraphQLObjectType create(
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
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description(
        "A group of routes which is generally known to the public by a similar name or number"
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("id")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .dataFetcher(environment ->
            Optional.ofNullable((AbstractTransitEntity<?, ?>) environment.getSource())
              .map(AbstractTransitEntity::getId)
              .map(idMapper::mapToApi)
              .orElse(null)
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("authority")
          .type(authorityType)
          .dataFetcher(environment -> (getSource(environment).getAgency()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("operator")
          .type(operatorType)
          .dataFetcher(environment -> ((getSource(environment)).getOperator()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("branding")
          .type(brandingType)
          .dataFetcher(environment -> (getSource(environment)).getBranding())
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("publicCode")
          .type(Scalars.GraphQLString)
          .description(
            "Publicly announced code for line, differentiating it from other lines for the same operator."
          )
          .dataFetcher(environment -> ((getSource(environment)).getShortName()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("name")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> (getSource(environment)).getLongName())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("transportMode")
          .type(EnumTypes.TRANSPORT_MODE)
          .dataFetcher(environment -> (getSource(environment)).getMode())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("transportSubmode")
          .type(EnumTypes.TRANSPORT_SUBMODE)
          .dataFetcher(environment ->
            TransmodelTransportSubmode.fromValue((getSource(environment)).getNetexSubmode())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("description")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> (getSource(environment)).getDescription())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition().name("url").type(Scalars.GraphQLString).build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("presentation")
          .type(presentationType)
          .dataFetcher(DataFetchingEnvironment::getSource)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikesAllowed")
          .type(EnumTypes.BIKES_ALLOWED)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("journeyPatterns")
          .type(new GraphQLList(journeyPatternType))
          .dataFetcher(environment ->
            GqlUtil.getTransitService(environment).findPatterns(getSource(environment))
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quays")
          .type(new GraphQLNonNull(new GraphQLList(quayType)))
          .dataFetcher(environment ->
            GqlUtil.getTransitService(environment)
              .findPatterns(getSource(environment))
              .stream()
              .map(TripPattern::getStops)
              .flatMap(Collection::stream)
              .distinct()
              .collect(Collectors.toList())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serviceJourneys")
          .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
          .dataFetcher(environment ->
            GqlUtil.getTransitService(environment)
              .findPatterns(getSource(environment))
              .stream()
              .flatMap(TripPattern::scheduledTripsAsStream)
              .distinct()
              .collect(Collectors.toList())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("notices")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(noticeType))))
          .dataFetcher(environment -> {
            Route route = getSource(environment);
            return GqlUtil.getTransitService(environment).findNotices(route);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("situations")
          .description("Get all situations active for the line.")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
          .dataFetcher(environment ->
            GqlUtil.getTransitService(environment)
              .getTransitAlertService()
              .getRouteAlerts((getSource(environment)).getId())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("flexibleLineType")
          .description("Type of flexible line, or null if line is not flexible.")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> (getSource(environment)).getFlexibleLineType())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
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
        GraphQLFieldDefinition.newFieldDefinition()
          .name("groupOfLines")
          .description("Groups of lines that line is a part of.")
          .type(new GraphQLNonNull(new GraphQLList(groupOfLinesType)))
          .dataFetcher(environment -> (getSource(environment)).getGroupsOfRoutes())
          .build()
      )
      .build();
  }

  private static Route getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
