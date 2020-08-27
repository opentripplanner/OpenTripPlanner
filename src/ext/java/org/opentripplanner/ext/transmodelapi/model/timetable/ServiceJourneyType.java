package org.opentripplanner.ext.transmodelapi.model.timetable;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.util.PolylineEncoder;

import java.util.stream.Collectors;

import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_SUBMODE;

public class ServiceJourneyType {
  private static final String NAME = "ServiceJourney";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create(
      GraphQLOutputType bookingArrangementType,
      GraphQLOutputType linkGeometryType,
      GraphQLOutputType operatorType,
      GraphQLOutputType noticeType,
      GraphQLOutputType quayType,
      GraphQLOutputType lineType,
      GraphQLOutputType ptSituationElementType,
      GraphQLOutputType journeyPatternType,
      GraphQLOutputType estimatedCallType,
      GraphQLOutputType timetabledPassingTimeType,
      GqlUtil gqlUtil
  ) {
    return GraphQLObjectType.newObject()
            .name(NAME)
            .description("A planned vehicle journey with passengers.")
            .field(GqlUtil.newTransitIdField())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("line")
                    .type(new GraphQLNonNull(lineType))
                    .dataFetcher(environment -> ((Trip) environment.getSource()).getRoute())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("activeDates")
                    .type(new GraphQLNonNull(new GraphQLList(gqlUtil.dateScalar)))
                    .dataFetcher(environment -> {
                      return GqlUtil
                          .getRoutingService(environment)
                          .getCalendarService()
                                  .getServiceDatesForServiceId((((Trip) environment.getSource()).getServiceId()))
                                  .stream().map(gqlUtil.serviceDateMapper::serviceDateToSecondsSinceEpoch).sorted().collect(
                              Collectors.toList());
                        }
                    )
                    .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("serviceAlteration")
//                        .type(serviceAlterationEnum)
//                        .description("Whether journey is as planned, a cancellation or an extra journey. Default is as planned")
//                        .dataFetcher(environment -> (((Trip) environment.getSource()).getServiceAlteration()))
//                        .build())

            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("transportSubmode")
                    .type(TRANSPORT_SUBMODE)
                    .description("The transport submode of the journey, if different from lines transport submode. NOT IMPLEMENTED")
                    .dataFetcher(environment -> TransmodelTransportSubmode.UNDEFINED)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("publicCode")
                    .type(Scalars.GraphQLString)
                    .description("Publicly announced code for service journey, differentiating it from other service journeys for the same line.")
                    .dataFetcher(environment -> (((Trip) environment.getSource()).getTripShortName()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("privateCode")
                    .type(Scalars.GraphQLString)
                    .description("For internal use by operators.")
                    .dataFetcher(environment -> (((Trip) environment.getSource()).getInternalPlanningCode()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("operator")
                    .type(operatorType)
                    .dataFetcher(
                            environment -> (((Trip) environment.getSource()).getOperator()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("directionType")
                    .type(EnumTypes.DIRECTION_TYPE)
                    .dataFetcher(environment -> directIdStringToInt(((Trip) environment.getSource()).getDirectionId()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("wheelchairAccessible")
                    .type(EnumTypes.WHEELCHAIR_BOARDING)
                    .description("Whether service journey is accessible with wheelchair.")
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("bikesAllowed")
                    .type(EnumTypes.BIKES_ALLOWED)
                    .description("Whether bikes are allowed on service journey.")
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("journeyPattern")
                    .type(journeyPatternType)
                    .dataFetcher(environment -> {
                      return GqlUtil.getRoutingService(environment).getPatternForTrip().get(environment.getSource());
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("quays")
                    .description("Quays visited by service journey")
                    .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(quayType))))
                    .dataFetcher(environment -> {
                      return GqlUtil.getRoutingService(environment).getPatternForTrip()
                              .get(environment.getSource()).getStops();
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("passingTimes")
                    .type(new GraphQLNonNull(new GraphQLList(timetabledPassingTimeType)))
                    .description("Returns scheduled passing times only - without realtime-updates, for realtime-data use 'estimatedCalls'")
                    .dataFetcher(environment -> {
                      return TripTimeShort.fromTripTimes(
                              GqlUtil.getRoutingService(environment).getPatternForTrip().get((Trip) environment.getSource()).scheduledTimetable,
                              environment.getSource());
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("estimatedCalls")
                    .type(new GraphQLList(estimatedCallType))
                    .description("Returns scheduled passingTimes for this ServiceJourney for a given date, updated with realtime-updates (if available). " +
                                         "NB! This takes a date as argument (default=today) and returns estimatedCalls for that date and should only be used if the date is " +
                                         "known when creating the request. For fetching estimatedCalls for a given trip.leg, use leg.serviceJourneyEstimatedCalls instead.")
                    .argument(GraphQLArgument.newArgument()
                            .name("date")
                            .type(gqlUtil.dateScalar)
                            .description("Date to get estimated calls for. Defaults to today.")
                            .defaultValue(null)
                            .build())
                    .dataFetcher(environment -> {
                        final Trip trip = environment.getSource();

                        final ServiceDate serviceDate = gqlUtil.serviceDateMapper.secondsSinceEpochToServiceDate(environment.getArgument("date"));
                      return GqlUtil
                          .getRoutingService(environment)
                          .getTripTimesShort(trip, serviceDate);
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("pointsOnLink")
                    .type(linkGeometryType)
                    .description("Detailed path travelled by service journey.")
                    .dataFetcher(environment -> {
                      LineString geometry = GqlUtil.getRoutingService(environment).getPatternForTrip()
                                        .get(environment.getSource())
                                        .getGeometry();
                                if (geometry == null) {
                                    return null;
                                }
                                return PolylineEncoder.createEncodings(geometry);
                            }
                    )
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("notices")
                    .type(new GraphQLNonNull(new GraphQLList(noticeType)))
                    .dataFetcher(environment -> {
                            Trip trip = environment.getSource();
                      return GqlUtil.getRoutingService(environment).getNoticesByEntity(trip);
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("situations")
                    .description("Get all situations active for the service journey.")
                    .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                    .dataFetcher(environment -> {
                      return GqlUtil.getRoutingService(environment).getTransitAlertService().getTripAlerts(
                      environment.getSource());
                    })
                .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("keyValues")
//                        .description("List of keyValue pairs for the service journey.")
//                        .type(new GraphQLList(keyValueType))
//                        .dataFetcher(environment -> ((Trip) environment.getSource()).getKeyValues())
//                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("flexibleServiceType")
//                        .description("Type of flexible service, or null if service is not flexible.")
//                        .type(flexibleServiceTypeEnum)
//                        .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("bookingArrangements")
                    .description("Booking arrangements for flexible services.")
                    .type(bookingArrangementType)
                    .build())
            .build();
  }

  private static int directIdStringToInt(String directionId) {
    try {
      return Integer.parseInt(directionId);
    } catch (NumberFormatException nfe) {
      return -1;
    }
  }
}
