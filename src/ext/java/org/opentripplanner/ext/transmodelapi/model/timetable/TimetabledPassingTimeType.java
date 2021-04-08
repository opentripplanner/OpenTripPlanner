package org.opentripplanner.ext.transmodelapi.model.timetable;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.TripTimeShort;

import static org.opentripplanner.model.StopPattern.PICKDROP_COORDINATE_WITH_DRIVER;
import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;

public class TimetabledPassingTimeType {
  private static final String NAME = "TimetabledPassingTime";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create(
      GraphQLOutputType bookingArrangementType,
      GraphQLOutputType noticeType,
      GraphQLOutputType quayType,
      GraphQLOutputType destinationDisplayType,
      GraphQLOutputType serviceJourneyType,
      GqlUtil gqlUtil
  ) {
    return GraphQLObjectType
        .newObject()
        .name(NAME)
        .description("Scheduled passing times. These are not affected by real time updates.")
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("quay")
            .type(quayType)
            .dataFetcher(environment -> {
              return GqlUtil.getRoutingService(environment).getStopForId((
                  (TripTimeShort) environment.getSource()
              ).getStopId());
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("arrival")
            .type(gqlUtil.timeScalar)
            .description("Scheduled time of arrival at quay")
            .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).getScheduledArrival())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("departure")
            .type(gqlUtil.timeScalar)
            .description("Scheduled time of departure from quay")
            .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).getScheduledDeparture())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("timingPoint")
            .type(Scalars.GraphQLBoolean)
            .description(
                "Whether this is a timing point or not. Boarding and alighting is not allowed at timing points.")
            .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).isTimepoint())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("forBoarding")
            .type(Scalars.GraphQLBoolean)
            .description("Whether vehicle may be boarded at quay.")
            .dataFetcher(environment -> {
              return GqlUtil.getRoutingService(environment)
                  .getPatternForTrip()
                  .get(((TripTimeShort) environment.getSource()).getTrip())
                  .getBoardType(((TripTimeShort) environment.getSource()).getStopIndex()) != PICKDROP_NONE;
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("forAlighting")
            .type(Scalars.GraphQLBoolean)
            .description("Whether vehicle may be alighted at quay.")
            .dataFetcher(environment -> {
              return GqlUtil.getRoutingService(environment)
                  .getPatternForTrip()
                  .get(((TripTimeShort) environment.getSource()).getTrip())
                  .getAlightType(((TripTimeShort) environment.getSource()).getStopIndex())
                  != PICKDROP_NONE;
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("requestStop")
            .type(Scalars.GraphQLBoolean)
            .description("Whether vehicle will only stop on request.")
            .dataFetcher(environment -> {
              return GqlUtil.getRoutingService(environment)
                  .getPatternForTrip()
                  .get(((TripTimeShort) environment.getSource()).getTrip())
                  .getAlightType(((TripTimeShort) environment.getSource()).getStopIndex())
                  == PICKDROP_COORDINATE_WITH_DRIVER;
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("serviceJourney")
            .type(serviceJourneyType)
            .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).getTrip())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("destinationDisplay")
            .type(destinationDisplayType)
            .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).getHeadsign())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("notices")
            .type(new GraphQLNonNull(new GraphQLList(noticeType)))
            .dataFetcher(environment -> {
              // TODO OTP2 - fix this
              // TripTimeShort tripTimeShort = environment.getSource();
              return null; //index.getNoticesByEntity(tripTimeShort.);
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("bookingArrangements")
            .description("Booking arrangements for flexible service. NOT IMPLEMENTED")
            .dataFetcher(environment -> null)
            .type(bookingArrangementType)
            .build())
        .build();
  }
}
