package org.opentripplanner.ext.transmodelapi.model.network;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.util.PolylineEncoder;

import java.util.BitSet;
import java.util.stream.Collectors;

public class JourneyPatternType {
  private static final String NAME = "JourneyPattern";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create(
      GraphQLOutputType linkGeometryType,
      GraphQLOutputType noticeType,
      GraphQLOutputType quayType,
      GraphQLOutputType lineType,
      GraphQLOutputType serviceJourneyType,
      GraphQLNamedOutputType ptSituationElementType,
      GqlUtil gqlUtil
  ) {
    return GraphQLObjectType.newObject()
        .name("JourneyPattern")
        .field(GqlUtil.newTransitIdField())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("line")
            .type(new GraphQLNonNull(lineType))
            .dataFetcher(environment -> ((TripPattern) environment.getSource()).route)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("directionType")
            .type(EnumTypes.DIRECTION_TYPE)
            .dataFetcher(environment -> ((TripPattern) environment.getSource()).getDirection())
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("name")
            .type(Scalars.GraphQLString)
            .dataFetcher(environment -> ((TripPattern) environment.getSource()).name)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("serviceJourneys")
            .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(serviceJourneyType))))
            .dataFetcher(environment -> ((TripPattern) environment.getSource()).getTrips())
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("serviceJourneysForDate")
            .description("List of service journeys for the journey pattern for a given date")
            .argument(GraphQLArgument.newArgument()
                .name("date")
                .type(gqlUtil.dateScalar)
                .build())
            .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(serviceJourneyType))))
            .dataFetcher(environment -> {

              BitSet services = GqlUtil.getRoutingService(environment)
                  .getServicesRunningForDate(
                      gqlUtil.serviceDateMapper.secondsSinceEpochToServiceDate(
                          environment.getArgument("date")
                      )
                  );
              return ((TripPattern) environment.getSource()).scheduledTimetable.tripTimes
                  .stream()
                  .filter(times -> services.get(times.serviceCode))
                  .map(times -> times.trip)
                  .collect(Collectors.toList());
            })
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("quays")
            .description("Quays visited by service journeys for this journey patterns")
            .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(quayType))))
            .dataFetcher(environment -> ((TripPattern) environment.getSource()).getStops())
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("pointsOnLink")
            .type(linkGeometryType)
            .dataFetcher(environment -> {
              LineString geometry = ((TripPattern) environment.getSource()).getGeometry();
              if (geometry == null) {
                return null;
              } else {
                return PolylineEncoder.createEncodings(geometry);
              }
            })
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("situations")
            .description("Get all situations active for the journey pattern.")
            .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
            .dataFetcher(environment -> {
              return GqlUtil.getRoutingService(environment).getTransitAlertService().getTripPatternAlerts(
                  environment.getSource());
            })
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("notices")
            .type(new GraphQLNonNull(new GraphQLList(noticeType)))
            .dataFetcher(environment -> {
              TripPattern tripPattern = environment.getSource();
              return GqlUtil.getRoutingService(environment).getNoticesByEntity(tripPattern);
            })
            .build())
        .build();
  }
}
