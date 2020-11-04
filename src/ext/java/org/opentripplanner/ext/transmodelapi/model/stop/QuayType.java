package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.ext.transmodelapi.model.plan.JourneyWhiteListed;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TripTimeShort;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_MODE;

public class QuayType {

  private static final String NAME = "Quay";
  public static final GraphQLOutputType REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create(
      GraphQLInterfaceType placeInterface,
      GraphQLOutputType stopPlaceType,
      GraphQLOutputType lineType,
      GraphQLOutputType journeyPatternType,
      GraphQLOutputType estimatedCallType,
      GraphQLOutputType ptSituationElementType,
      GraphQLOutputType tariffZoneType,
      GqlUtil gqlUtil
      ) {
    return GraphQLObjectType.newObject()
            .name(NAME)
            .description("A place such as platform, stance, or quayside where passengers have access to PT vehicles.")
            .withInterface(placeInterface)
            .field(GqlUtil.newTransitIdField())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("name")
                    .type(new GraphQLNonNull(Scalars.GraphQLString))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("latitude")
                    .type(Scalars.GraphQLFloat)
                    .dataFetcher(environment -> (((Stop) environment.getSource()).getLat()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("longitude")
                    .type(Scalars.GraphQLFloat)
                    .dataFetcher(environment -> (((Stop) environment.getSource()).getLon()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("description")
                    .type(Scalars.GraphQLString)
                    .dataFetcher(environment -> (((Stop) environment.getSource()).getDescription()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("stopPlace")
                .description("The stop place to which this quay belongs to.")
                .type(stopPlaceType)
                .dataFetcher(environment ->
                    {
                        Station station = ((Stop) environment.getSource()).getParentStation();
                        if (station != null) {
                          return new MonoOrMultiModalStation(
                                station,
                                GqlUtil
                                    .getRoutingService(environment)
                                    .getMultiModalStationForStations().get(station));
                        } else {
                            return null;
                        }
                    }
                )
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("wheelchairAccessible")
                    .type(EnumTypes.WHEELCHAIR_BOARDING)
                    .description("Whether this quay is suitable for wheelchair boarding.")
                    .dataFetcher(environment -> (((Stop) environment.getSource()).getWheelchairBoarding()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("publicCode")
                    .type(Scalars.GraphQLString)
                    .description("Public code used to identify this quay within the stop place. For instance a platform code.")
                    .dataFetcher(environment -> (((Stop) environment.getSource()).getCode()))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("lines")
                    .description("List of lines servicing this quay")
                    .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(lineType))))
                    .dataFetcher(environment -> {
                      return GqlUtil.getRoutingService(environment)
                          .getPatternsForStop(environment.getSource(),true)
                              .stream()
                              .map(pattern -> pattern.route)
                              .distinct()
                              .collect(Collectors.toList());
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("journeyPatterns")
                    .description("List of journey patterns servicing this quay")
                    .type(new GraphQLNonNull(new GraphQLList(journeyPatternType)))
                    .dataFetcher(environment -> {
                      return GqlUtil
                          .getRoutingService(environment)
                          .getPatternsForStop(environment.getSource(), true);
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("estimatedCalls")
                    .description("List of visits to this quay as part of vehicle journeys.")
                    .type(new GraphQLNonNull(new GraphQLList(estimatedCallType)))
                    .argument(GraphQLArgument.newArgument()
                            .name("startTime")
                            .type(gqlUtil.dateTimeScalar)
                            .description("DateTime for when to fetch estimated calls from. Default value is current time")
                            .build())
                    .argument(GraphQLArgument.newArgument()
                            .name("timeRange")
                            .type(Scalars.GraphQLInt)
                            .defaultValue(24 * 60 * 60)
                            .build())
                    .argument(GraphQLArgument.newArgument()
                            .name("numberOfDepartures")
                            .description("Limit the total number of departures returned.")
                            .type(Scalars.GraphQLInt)
                            .defaultValue(5)
                            .build())
                    .argument(GraphQLArgument.newArgument()
                            .name("numberOfDeparturesPerLineAndDestinationDisplay")
                            .description("Limit the number of departures per line and destination display returned. The parameter is only applied " +
                                    "when the value is between 1 and 'numberOfDepartures'.")
                            .type(Scalars.GraphQLInt)
                            .build())
                    .argument(GraphQLArgument.newArgument()
                            .name("omitNonBoarding")
                            .type(Scalars.GraphQLBoolean)
                            .defaultValue(false)
                            .build())
                    .argument(GraphQLArgument.newArgument()
                            .name("whiteListed")
                            .description("Whitelisted")
                            .description("Parameters for indicating the only authorities and/or lines or quays to list estimatedCalls for")
                            .type(JourneyWhiteListed.INPUT_TYPE)
                            .build())
                    .argument(GraphQLArgument.newArgument()
                          .name("whiteListedModes")
                          .description("Only show estimated calls for selected modes.")
                          .type(GraphQLList.list(TRANSPORT_MODE))
                          .build())
                    .argument(GraphQLArgument.newArgument()
                        .name("includeCancelledTrips")
                        .description("Indicates that realtime-cancelled trips should also be included. NOT IMPLEMENTED")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(false)
                        .build())
                    .dataFetcher(environment -> {
                        boolean omitNonBoarding = environment.getArgument("omitNonBoarding");
                        int numberOfDepartures = environment.getArgument("numberOfDepartures");
                        Integer departuresPerLineAndDestinationDisplay = environment.getArgument("numberOfDeparturesPerLineAndDestinationDisplay");
                        int timeRange = environment.getArgument("timeRange");
                        Stop stop = environment.getSource();

                        JourneyWhiteListed whiteListed = new JourneyWhiteListed(environment);
                        Collection<TransitMode> transitModes = environment.getArgument("whiteListedModes");

                        Long startTimeMs = environment.getArgument("startTime") == null ? 0L : environment.getArgument("startTime");
                        Long startTimeSeconds = startTimeMs / 1000;

                      return StopPlaceType.getTripTimesForStop(
                          stop,
                          startTimeSeconds,
                          timeRange,
                          omitNonBoarding,
                          numberOfDepartures,
                          departuresPerLineAndDestinationDisplay,
                          whiteListed.authorityIds,
                          whiteListed.lineIds,
                          transitModes,
                          environment
                      )
                            .sorted(TripTimeShort.compareByDeparture())
                            .distinct()
                            .limit(numberOfDepartures)
                            .collect(Collectors.toList());
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("situations")
                    .description("Get all situations active for the quay.")
                    .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                .dataFetcher(env -> {
                  return GqlUtil.getRoutingService(env).getTransitAlertService()
                      .getStopAlerts(((Stop)env.getSource()).getId());
                })
                    .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("stopType")
//                        .type(stopTypeEnum)
//                        .dataFetcher(environment -> (((Stop) environment.getSource()).getStopType()))
//                        .build())
           .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("tariffZones")
                    .type(new GraphQLNonNull(new GraphQLList(tariffZoneType)))
                    .dataFetcher(environment -> ((Stop) environment.getSource()).getFareZones())
                    .build())
           .build();
  }
}
