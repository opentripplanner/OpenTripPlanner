package org.opentripplanner.apis.transmodel.model.plan;

import static org.opentripplanner.apis.transmodel.model.EnumTypes.ALTERNATIVE_LEGS_FILTER;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.LEG_MODE;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.apis.transmodel.model.TripTimeOnDateHelper;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.framework.geometry.EncodedPolyline;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.model.plan.leg.StopArrival;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.model.plan.legreference.LegReferenceSerializer;
import org.opentripplanner.routing.alternativelegs.AlternativeLegs;
import org.opentripplanner.routing.alternativelegs.NavigationDirection;

public class LegType {

  private static final String NAME = "Leg";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

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
    GraphQLOutputType datedServiceJourneyType,
    GraphQLOutputType ptSituationElementType,
    GraphQLObjectType placeType,
    GraphQLObjectType pathGuidanceType,
    GraphQLType elevationStepType,
    GraphQLScalarType dateTimeScalar
  ) {
    return GraphQLObjectType.newObject()
      .name("Leg")
      .description(
        "Part of a trip pattern. Either a ride on a public transport vehicle or access or path link to/from/between places"
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("id")
          .description(
            "An identifier for the leg, which can be used to re-fetch transit leg information."
          )
          .type(Scalars.GraphQLID)
          .dataFetcher(env -> LegReferenceSerializer.encode(leg(env).legReference()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("aimedStartTime")
          .description("The aimed date and time this leg starts.")
          .type(new GraphQLNonNull(dateTimeScalar))
          .dataFetcher(env ->
            // startTime is already adjusted for real-time - need to subtract delay to get aimed time
            leg(env).startTime().minusSeconds(leg(env).departureDelay()).toInstant().toEpochMilli()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("expectedStartTime")
          .description("The expected, real-time adjusted date and time this leg starts.")
          .type(new GraphQLNonNull(dateTimeScalar))
          .dataFetcher(env -> leg(env).startTime().toInstant().toEpochMilli())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("aimedEndTime")
          .description("The aimed date and time this leg ends.")
          .type(new GraphQLNonNull(dateTimeScalar))
          .dataFetcher(env -> // endTime is already adjusted for real-time - need to subtract delay to get aimed time
            leg(env).endTime().minusSeconds(leg(env).arrivalDelay()).toInstant().toEpochMilli()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("expectedEndTime")
          .description("The expected, real-time adjusted date and time this leg ends.")
          .type(new GraphQLNonNull(dateTimeScalar))
          .dataFetcher(env -> leg(env).endTime().toInstant().toEpochMilli())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("mode")
          .description(
            "The mode of transport or access (e.g., foot) used when traversing this leg."
          )
          .type(new GraphQLNonNull(LEG_MODE))
          .dataFetcher(env -> onLeg(env, StreetLeg::getMode, TransitLeg::mode))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("transportSubmode")
          .description(
            "The transport sub mode (e.g., localBus or expressBus) used when traversing this leg. Null if leg is not a ride"
          )
          .type(EnumTypes.TRANSPORT_SUBMODE)
          .dataFetcher(environment ->
            ((Leg) environment.getSource()).trip() != null
              ? TransmodelTransportSubmode.fromValue(
                ((Leg) environment.getSource()).trip().getNetexSubMode()
              )
              : null
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("duration")
          .description("The leg's duration in seconds")
          .type(new GraphQLNonNull(ExtendedScalars.GraphQLLong))
          .dataFetcher(env -> leg(env).duration().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("directDuration")
          .type(new GraphQLNonNull(ExtendedScalars.GraphQLLong))
          .description("NOT IMPLEMENTED")
          .dataFetcher(env -> leg(env).duration().toSeconds())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("pointsOnLink")
          .description("The leg's geometry.")
          .type(linkGeometryType)
          .dataFetcher(env -> EncodedPolyline.encode(leg(env).legGeometry()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("authority")
          .description(
            "For ride legs, the service authority used for this legs. For non-ride legs, null."
          )
          .type(authorityType)
          .dataFetcher(env -> leg(env).agency())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("operator")
          .description("For ride legs, the operator used for this legs. For non-ride legs, null.")
          .type(operatorType)
          .dataFetcher(env -> leg(env).operator())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("realtime")
          .description("Whether there is real-time data about this leg")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .dataFetcher(env -> leg(env).isRealTimeUpdated())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("distance")
          .description("The distance traveled while traversing the leg in meters.")
          .type(new GraphQLNonNull(Scalars.GraphQLFloat))
          .dataFetcher(env -> leg(env).distanceMeters())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("generalizedCost")
          .description("Generalized cost or weight of the leg. Used for debugging.")
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> leg(env).generalizedCost())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("ride")
          .description("Whether this leg is a ride leg or not.")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .dataFetcher(env -> leg(env).isTransitLeg())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("walkingBike")
          .description("Whether this leg is walking with a bike.")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(env -> leg(env).walkingBike())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("rentedBike")
          .description("Whether this leg is with a rented bike.")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(env -> leg(env).rentedVehicle())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("fromPlace")
          .description("The Place where the leg originates.")
          .type(new GraphQLNonNull(placeType))
          .dataFetcher(env -> leg(env).from())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("toPlace")
          .description("The Place where the leg ends.")
          .type(new GraphQLNonNull(placeType))
          .dataFetcher(env -> leg(env).to())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("fromEstimatedCall")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .description("EstimatedCall for the quay where the leg originates.")
          .type(estimatedCallType)
          .dataFetcher(env -> TripTimeOnDateHelper.getTripTimeOnDateForFromPlace(env.getSource()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("toEstimatedCall")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .description("EstimatedCall for the quay where the leg ends.")
          .type(estimatedCallType)
          .dataFetcher(env -> TripTimeOnDateHelper.getTripTimeOnDateForToPlace(env.getSource()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("line")
          .description("For ride legs, the line. For non-ride legs, null.")
          .type(lineType)
          .dataFetcher(env -> leg(env).route())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serviceJourney")
          .description("For ride legs, the service journey. For non-ride legs, null.")
          .type(serviceJourneyType)
          .dataFetcher(env -> leg(env).trip())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("datedServiceJourney")
          .description("The dated service journey used for this leg.")
          .type(datedServiceJourneyType)
          .dataFetcher(env -> leg(env).tripOnServiceDate())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serviceDate")
          .description(
            "For transit legs, the service date of the trip. For non-transit legs, null."
          )
          .type(TransmodelScalars.DATE_SCALAR)
          .dataFetcher(environment ->
            Optional.of((Leg) environment.getSource()).map(Leg::serviceDate).orElse(null)
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("intermediateQuays")
          .description(
            "For ride legs, intermediate quays between the Place where the leg originates and the Place where the leg ends. For non-ride legs, empty list."
          )
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(quayType))))
          .dataFetcher(env -> {
            List<StopArrival> stops = ((Leg) env.getSource()).listIntermediateStops();
            if (stops == null || stops.isEmpty()) {
              return List.of();
            } else {
              return (
                stops
                  .stream()
                  .map(stop -> stop.place.stop)
                  .filter(Objects::nonNull)
                  .collect(Collectors.toList())
              );
            }
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("intermediateEstimatedCalls")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .description(
            "For ride legs, estimated calls for quays between the Place where the leg originates and the Place where the leg ends. For non-ride legs, empty list."
          )
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(estimatedCallType))))
          .dataFetcher(env ->
            TripTimeOnDateHelper.getIntermediateTripTimeOnDatesForLeg(env.getSource())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serviceJourneyEstimatedCalls")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .description(
            "For ride legs, all estimated calls for the service journey. For non-ride legs, empty list."
          )
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(estimatedCallType))))
          .dataFetcher(env -> TripTimeOnDateHelper.getAllTripTimeOnDatesForLegsTrip(env.getSource())
          )
          .build()
      )
      //                .field(GraphQLFieldDefinition.newFieldDefinition()
      //                        .name("via")
      //                        .description("Do we continue from a specified via place")
      //                        .type(Scalars.GraphQLBoolean)
      //                        .dataFetcher(env -> leg(env).intermediatePlace)
      //                        .build())
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("situations")
          .description("All relevant situations for this leg")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
          .dataFetcher(env -> leg(env).listTransitAlerts())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("steps")
          .description("Do we continue from a specified via place")
          .type(new GraphQLNonNull(new GraphQLList(pathGuidanceType)))
          .dataFetcher(env -> leg(env).listWalkSteps())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("interchangeFrom")
          .type(interchangeType)
          .dataFetcher(env -> leg(env).transferFromPrevLeg())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("interchangeTo")
          .type(interchangeType)
          .dataFetcher(env -> leg(env).transferToNextLeg())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bookingArrangements")
          .type(bookingArrangementType)
          .dataFetcher(env -> leg(env).pickupBookingInfo())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bikeRentalNetworks")
          .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
          .dataFetcher(env ->
            leg(env).vehicleRentalNetwork() == null
              ? List.of()
              : List.of(leg(env).vehicleRentalNetwork())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .description(ElevationProfileStepType.makeDescription("leg"))
          .name("elevationProfile")
          .type(new GraphQLNonNull(new GraphQLList(elevationStepType)))
          .dataFetcher(env ->
            ElevationProfileStepType.mapElevationProfile(leg(env).elevationProfile())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("previousLegs")
          .description(
            "Fetch the previous legs, which can be used to replace this leg. The replacement legs do arrive/depart from/to the same stop places. It might be necessary to change other legs in an itinerary in order to be able to ride the returned legs."
          )
          .type(new GraphQLList(new GraphQLNonNull(REF)))
          .argument(
            GraphQLArgument.newArgument()
              .name("previous")
              .description("Number of earlier legs to return.")
              .defaultValueProgrammatic(1)
              .type(Scalars.GraphQLInt)
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("filter")
              .description("Whether the leg should be similar to this leg in some way.")
              .defaultValueProgrammatic("noFilter")
              .type(ALTERNATIVE_LEGS_FILTER)
              .build()
          )
          .dataFetcher(env -> {
            Leg leg = leg(env);
            if (!leg.isScheduledTransitLeg()) {
              return null;
            }
            return AlternativeLegs.getAlternativeLegs(
              leg,
              env.getArgument("previous"),
              GqlUtil.getTransitService(env),
              NavigationDirection.PREVIOUS,
              env.getArgument("filter")
            );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("nextLegs")
          .description(
            "Fetch the next legs, which can be used to replace this leg. The replacement legs do arrive/depart from/to the same stop places. It might be necessary to change other legs in an itinerary in order to be able to ride the returned legs."
          )
          .type(new GraphQLList(new GraphQLNonNull(REF)))
          .argument(
            GraphQLArgument.newArgument()
              .name("next")
              .description("Number of later legs to return.")
              .defaultValueProgrammatic(1)
              .type(Scalars.GraphQLInt)
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("filter")
              .description("Whether the leg should be similar to this leg in some way.")
              .defaultValueProgrammatic("noFilter")
              .type(ALTERNATIVE_LEGS_FILTER)
              .build()
          )
          .dataFetcher(env -> {
            Leg leg = leg(env);
            if (!leg.isScheduledTransitLeg()) {
              return null;
            }
            return AlternativeLegs.getAlternativeLegs(
              leg,
              env.getArgument("next"),
              GqlUtil.getTransitService(env),
              NavigationDirection.NEXT,
              env.getArgument("filter")
            );
          })
          .build()
      )
      .build();
  }

  private static Leg leg(DataFetchingEnvironment environment) {
    return environment.getSource();
  }

  private static <S, T> Object onLeg(
    DataFetchingEnvironment environment,
    Function<StreetLeg, S> streetLegAccessor,
    Function<TransitLeg, T> transitLegAccessor
  ) {
    Leg leg = leg(environment);
    if (leg instanceof StreetLeg sl) {
      return streetLegAccessor.apply(sl);
    }
    if (leg instanceof TransitLeg tl) {
      return transitLegAccessor.apply(tl);
    }
    throw new IllegalStateException("Unhandled leg type: " + leg);
  }
}
