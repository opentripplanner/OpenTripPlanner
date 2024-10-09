package org.opentripplanner.apis.transmodel.model.network;

import gnu.trove.set.TIntSet;
import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.apis.transmodel.mapping.GeometryMapper;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.framework.geometry.EncodedPolyline;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class JourneyPatternType {

  private static final String NAME = "JourneyPattern";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create(
    GraphQLOutputType linkGeometryType,
    GraphQLOutputType noticeType,
    GraphQLOutputType quayType,
    GraphQLOutputType lineType,
    GraphQLOutputType serviceJourneyType,
    GraphQLOutputType stopToStopGeometryType,
    GraphQLNamedOutputType ptSituationElementType,
    GqlUtil gqlUtil
  ) {
    return GraphQLObjectType
      .newObject()
      .name("JourneyPattern")
      .field(GqlUtil.newTransitIdField())
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("line")
          .type(new GraphQLNonNull(lineType))
          .dataFetcher(environment -> ((TripPattern) environment.getSource()).getRoute())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("directionType")
          .type(EnumTypes.DIRECTION_TYPE)
          .dataFetcher(environment -> ((TripPattern) environment.getSource()).getDirection())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("name")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((TripPattern) environment.getSource()).getName())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("serviceJourneys")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(serviceJourneyType))))
          .dataFetcher(e ->
            ((TripPattern) e.getSource()).scheduledTripsAsStream().collect(Collectors.toList())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("serviceJourneysForDate")
          .withDirective(gqlUtil.timingData)
          .description("List of service journeys for the journey pattern for a given date")
          .argument(GraphQLArgument.newArgument().name("date").type(gqlUtil.dateScalar).build())
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(serviceJourneyType))))
          .dataFetcher(environment -> {
            TIntSet services = GqlUtil
              .getTransitService(environment)
              .getServiceCodesRunningForDate(
                Optional
                  .ofNullable((LocalDate) environment.getArgument("date"))
                  .orElse(LocalDate.now())
              );

            return ((TripPattern) environment.getSource()).getScheduledTimetable()
              .getTripTimes()
              .stream()
              .filter(times -> services.contains(times.getServiceCode()))
              .map(TripTimes::getTrip)
              .collect(Collectors.toList());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("quays")
          .description("Quays visited by service journeys for this journey patterns")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(quayType))))
          .dataFetcher(environment -> ((TripPattern) environment.getSource()).getStops())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("pointsOnLink")
          .type(linkGeometryType)
          .dataFetcher(environment -> {
            LineString geometry = ((TripPattern) environment.getSource()).getGeometry();
            if (geometry == null) {
              return null;
            } else {
              return EncodedPolyline.encode(geometry);
            }
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("stopToStopGeometries")
          .description(
            "Detailed path travelled by journey pattern divided into stop-to-stop sections."
          )
          .type(new GraphQLList(stopToStopGeometryType))
          .dataFetcher(environment ->
            GeometryMapper.mapStopToStopGeometries(environment.getSource())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("situations")
          .description("Get all situations active for the journey pattern.")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
          .dataFetcher(environment -> {
            TripPattern tripPattern = environment.getSource();
            return GqlUtil
              .getTransitService(environment)
              .getTransitAlertService()
              .getDirectionAndRouteAlerts(
                tripPattern.getDirection(),
                tripPattern.getRoute().getId()
              );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("notices")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(noticeType))))
          .dataFetcher(environment -> {
            TripPattern tripPattern = environment.getSource();
            return GqlUtil.getTransitService(environment).getNoticesByEntity(tripPattern);
          })
          .build()
      )
      .build();
  }
}
