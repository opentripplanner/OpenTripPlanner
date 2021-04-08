package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.ext.transmodelapi.model.TripTimeShortHelper;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StopArrival;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.MODE;

public class LegType {
  public static GraphQLObjectType create(
      GraphQLOutputType bookingArrangementType,
      GraphQLOutputType interchangeType,
      GraphQLOutputType linkGeometryType,
      GraphQLOutputType authorityType,
      GraphQLOutputType operatorType,
      GraphQLOutputType quayType,
      GraphQLOutputType estimatedCallType,
      GraphQLOutputType lineType,
      GraphQLOutputType serviceJourneyType,
      GraphQLOutputType ptSituationElementType,
      GraphQLObjectType placeType,
      GraphQLObjectType pathGuidanceType,
      GqlUtil gqlUtil

  ) {
    return GraphQLObjectType
        .newObject()
        .name("Leg")
        .description(
            "Part of a trip pattern. Either a ride on a public transport vehicle or access or path link to/from/between places")
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("aimedStartTime")
            .description("The aimed date and time this leg starts.")
            .type(gqlUtil.dateTimeScalar)
            .dataFetcher(
                // startTime is already adjusted for realtime - need to subtract delay to get aimed time
                environment -> ((Leg) environment.getSource()).startTime.getTimeInMillis() - (
                    1000 * ((Leg) environment.getSource()).departureDelay
                ))
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("expectedStartTime")
            .description("The expected, realtime adjusted date and time this leg starts.")
            .type(gqlUtil.dateTimeScalar)
            .dataFetcher(environment -> ((Leg) environment.getSource()).startTime.getTimeInMillis())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("aimedEndTime")
            .description("The aimed date and time this leg ends.")
            .type(gqlUtil.dateTimeScalar)
            .dataFetcher(
                // endTime is already adjusted for realtime - need to subtract delay to get aimed time
                environment -> ((Leg) environment.getSource()).endTime.getTimeInMillis() - (
                    1000 * ((Leg) environment.getSource()).arrivalDelay
                ))
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("expectedEndTime")
            .description("The expected, realtime adjusted date and time this leg ends.")
            .type(gqlUtil.dateTimeScalar)
            .dataFetcher(environment -> ((Leg) environment.getSource()).endTime.getTimeInMillis())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("mode")
            .description(
                "The mode of transport or access (e.g., foot) used when traversing this leg.")
            .type(MODE)
            .dataFetcher(environment -> ((Leg) environment.getSource()).mode)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("transportSubmode")
            .description(
                "The transport sub mode (e.g., localBus or expressBus) used when traversing this leg. Null if leg is not a ride")
            .type(EnumTypes.TRANSPORT_SUBMODE)
            .dataFetcher(environment -> TransmodelTransportSubmode.UNDEFINED)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("duration")
            .description("The legs's duration in seconds")
            .type(Scalars.GraphQLLong)
            .dataFetcher(environment -> ((Leg) environment.getSource()).getDuration())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("directDuration")
            .type(Scalars.GraphQLLong)
            .description("NOT IMPLEMENTED")
            .dataFetcher(environment -> ((Leg) environment.getSource()).getDuration())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("pointsOnLink")
            .description("The legs's geometry.")
            .type(linkGeometryType)
            .dataFetcher(environment -> ((Leg) environment.getSource()).legGeometry)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("authority")
            .description(
                "For ride legs, the service authority used for this legs. For non-ride legs, null.")
            .type(authorityType)
            .dataFetcher(environment -> ((Leg) environment.getSource()).getAgency())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("operator")
            .description("For ride legs, the operator used for this legs. For non-ride legs, null.")
            .type(operatorType)
            .dataFetcher(environment -> ((Leg) environment.getSource()).getOperator())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("realtime")
            .description("Whether there is real-time data about this leg")
            .type(Scalars.GraphQLBoolean)
            .dataFetcher(environment -> ((Leg) environment.getSource()).realTime)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("distance")
            .description("The distance traveled while traversing the leg in meters.")
            .type(Scalars.GraphQLFloat)
            .dataFetcher(environment -> ((Leg) environment.getSource()).distanceMeters)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("generalizedCost")
            .description("Generalized cost or weight of the leg. Used for debugging.")
            .type(Scalars.GraphQLInt)
            .dataFetcher(environment -> ((Leg) environment.getSource()).generalizedCost)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("ride")
            .description("Whether this leg is a ride leg or not.")
            .type(Scalars.GraphQLBoolean)
            .dataFetcher(environment -> ((Leg) environment.getSource()).isTransitLeg())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("rentedBike")
            .description("Whether this leg is with a rented bike.")
            .type(Scalars.GraphQLBoolean)
            .dataFetcher(environment -> ((Leg) environment.getSource()).rentedBike)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("fromPlace")
            .description("The Place where the leg originates.")
            .type(new GraphQLNonNull(placeType))
            .dataFetcher(environment -> ((Leg) environment.getSource()).from)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("toPlace")
            .description("The Place where the leg ends.")
            .type(new GraphQLNonNull(placeType))
            .dataFetcher(environment -> ((Leg) environment.getSource()).to)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("fromEstimatedCall")
            .description("EstimatedCall for the quay where the leg originates.")
            .type(estimatedCallType)
            .dataFetcher(environment -> TripTimeShortHelper.getTripTimeShortForFromPlace(environment
                .getSource(), GqlUtil.getRoutingService(environment)))
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("toEstimatedCall")
            .description("EstimatedCall for the quay where the leg ends.")
            .type(estimatedCallType)
            .dataFetcher(environment -> TripTimeShortHelper.getTripTimeShortForToPlace(
                environment.getSource(),
                GqlUtil.getRoutingService(environment)
            ))
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("line")
            .description("For ride legs, the line. For non-ride legs, null.")
            .type(lineType)
            .dataFetcher(environment -> ((Leg) environment.getSource()).getRoute())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("serviceJourney")
            .description("For ride legs, the service journey. For non-ride legs, null.")
            .type(serviceJourneyType)
            .dataFetcher(environment -> ((Leg) environment.getSource()).getTrip())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("intermediateQuays")
            .description(
                "For ride legs, intermediate quays between the Place where the leg originates and the Place where the leg ends. For non-ride legs, empty list.")
            .type(new GraphQLNonNull(new GraphQLList(quayType)))
            .dataFetcher(environment -> {
              List<StopArrival> stops = ((Leg) environment.getSource()).intermediateStops;
              if (stops == null || stops.isEmpty()) {
                return List.of();
              }
              else {
                return (
                    stops.stream().filter(stop -> stop.place.stopId != null).map(s -> {
                      return GqlUtil.getRoutingService(environment).getStopForId(s.place.stopId);
                    }).filter(Objects::nonNull).collect(Collectors.toList())
                );
              }
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("intermediateEstimatedCalls")
            .description(
                "For ride legs, estimated calls for quays between the Place where the leg originates and the Place where the leg ends. For non-ride legs, empty list."
            )
            .type(new GraphQLNonNull(new GraphQLList(estimatedCallType)))
            .dataFetcher(environment -> TripTimeShortHelper.getIntermediateTripTimeShortsForLeg((
                environment.getSource()
            ), GqlUtil.getRoutingService(environment)))
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("serviceJourneyEstimatedCalls")
            .description(
                "For ride legs, all estimated calls for the service journey. For non-ride legs, empty list.")
            .type(new GraphQLNonNull(new GraphQLList(estimatedCallType)))
            .dataFetcher(environment ->
                TripTimeShortHelper.getAllTripTimeShortsForLegsTrip(
                    environment.getSource(),
                    GqlUtil.getRoutingService(environment)
                )
            )
            .build())
        //                .field(GraphQLFieldDefinition.newFieldDefinition()
        //                        .name("via")
        //                        .description("Do we continue from a specified via place")
        //                        .type(Scalars.GraphQLBoolean)
        //                        .dataFetcher(environment -> ((Leg) environment.getSource()).intermediatePlace)
        //                        .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("situations")
            .description("All relevant situations for this leg")
            .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
            .dataFetcher(environment -> ((Leg) environment.getSource()).transitAlerts)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("steps")
            .description("Do we continue from a specified via place")
            .type(new GraphQLNonNull(new GraphQLList(pathGuidanceType)))
            .dataFetcher(environment -> ((Leg) environment.getSource()).walkSteps)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("interchangeFrom")
            .description("NOT IMPLEMENTED")
            .type(interchangeType)
            .dataFetcher(environment -> null)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("interchangeTo")
            .description("NOT IMPLEMENTED")
            .type(interchangeType)
            .dataFetcher(environment -> null)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("bookingArrangements")
            .type(bookingArrangementType)
            .dataFetcher(environment -> ((Leg) environment.getSource()).bookingInfo)
            .build())
        .build();
  }
}
