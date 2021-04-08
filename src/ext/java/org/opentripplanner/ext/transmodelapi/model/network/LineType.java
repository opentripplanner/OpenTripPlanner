package org.opentripplanner.ext.transmodelapi.model.network;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.util.OTPFeature;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_MODE;

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
      GraphQLOutputType ptSituationElementType
  ) {
    return GraphQLObjectType.newObject()
            .name(NAME)
            .description("A group of routes which is generally known to the public by a similar name or number")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLID))
                    .dataFetcher(environment -> TransitIdMapper.mapEntityIDToApi(environment.getSource()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("authority")
                    .type(authorityType)
                    .dataFetcher(environment -> (((Route) environment.getSource()).getAgency()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("operator")
                    .type(operatorType)
                    .dataFetcher(environment -> (((Route) environment.getSource()).getOperator()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("publicCode")
                    .type(Scalars.GraphQLString)
                    .description("Publicly announced code for line, differentiating it from other lines for the same operator.")
                    .dataFetcher(environment -> (((Route) environment.getSource()).getShortName()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("name")
                    .type(Scalars.GraphQLString)
                    .dataFetcher(environment -> ((Route) environment.getSource()).getLongName())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("transportMode")
                    .type(TRANSPORT_MODE)
                    .dataFetcher(environment -> ((Route)environment.getSource()).getMode())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("transportSubmode")
                    .type(EnumTypes.TRANSPORT_SUBMODE)
                    .description("NOT IMPLEMENTED")
                    .dataFetcher(environment -> TransmodelTransportSubmode.UNDEFINED)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("description")
                    .type(Scalars.GraphQLString)
                    .dataFetcher(environment -> ((Route) environment.getSource()).getDesc())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("url")
                    .type(Scalars.GraphQLString)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("presentation")
                    .type(presentationType)
                    .dataFetcher(DataFetchingEnvironment::getSource)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("bikesAllowed")
                    .type(EnumTypes.BIKES_ALLOWED)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("journeyPatterns")
                    .type(new GraphQLList(journeyPatternType))
                    .dataFetcher(environment -> {
                      return GqlUtil.getRoutingService(environment).getPatternsForRoute()
                              .get(environment.getSource());
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("quays")
                    .type(new GraphQLNonNull(new GraphQLList(quayType)))
                    .dataFetcher(environment -> {
                      return GqlUtil.getRoutingService(environment).getPatternsForRoute()
                              .get(environment.getSource())
                              .stream()
                              .map(TripPattern::getStops)
                              .flatMap(Collection::stream)
                              .distinct()
                              .collect(Collectors.toList());
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("serviceJourneys")
                    .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
                    .dataFetcher(environment -> {
                      List<Trip> result = GqlUtil
                          .getRoutingService(environment)
                          .getPatternsForRoute()
                          .get(environment.getSource())
                          .stream()
                          .map(TripPattern::getTrips)
                          .flatMap(Collection::stream)
                          .distinct()
                          .collect(Collectors.toList());

                      if(OTPFeature.FlexRouting.isOn()) {
                        // Workaround since flex trips are not part of patterns yet
                        result.addAll(GqlUtil.getRoutingService(environment).getFlexIndex().tripById
                            .values()
                            .stream()
                            .filter(t -> t.getRoute().equals((Route) environment.getSource()))
                            .collect(Collectors.toList()));
                      }
                      return result;
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("notices")
                    .type(new GraphQLNonNull(new GraphQLList(noticeType)))
                    .dataFetcher(environment -> {
                        Route route = environment.getSource();
                      return GqlUtil.getRoutingService(environment).getNoticesByEntity(route);
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("situations")
                    .description("Get all situations active for the line.")
                    .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                .dataFetcher(environment -> {
                  return GqlUtil.getRoutingService(environment).getTransitAlertService().getRouteAlerts(
                      environment.getSource());
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("flexibleLineType")
                .description("Type of flexible line, or null if line is not flexible.")
                .type(Scalars.GraphQLString)
                .dataFetcher(environment -> ((Route) environment.getSource()).getFlexibleLineType())
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("bookingArrangements")
                .description("Booking arrangements for flexible line.")
                .type(bookingArrangementType)
                .dataFetcher(environment -> null)
                .build())
            .build();
  }
}
