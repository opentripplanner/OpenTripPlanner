package org.opentripplanner.ext.transmodelapi.model.timetable;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TripTimeOnDate;

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
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("quay")
          .type(quayType)
          .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).getStop())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("arrival")
          .type(gqlUtil.timeScalar)
          .description("Scheduled time of arrival at quay")
          .dataFetcher(environment ->
            missingValueToNull(((TripTimeOnDate) environment.getSource()).getScheduledArrival())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("departure")
          .type(gqlUtil.timeScalar)
          .description("Scheduled time of departure from quay")
          .dataFetcher(environment ->
            missingValueToNull(((TripTimeOnDate) environment.getSource()).getScheduledDeparture())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("timingPoint")
          .type(Scalars.GraphQLBoolean)
          .description(
            "Whether this is a timing point or not. Boarding and alighting is not allowed at timing points."
          )
          .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).isTimepoint())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("forBoarding")
          .type(Scalars.GraphQLBoolean)
          .description("Whether vehicle may be boarded at quay.")
          .dataFetcher(environment ->
            ((TripTimeOnDate) environment.getSource()).getPickupType() != PickDrop.NONE
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("forAlighting")
          .type(Scalars.GraphQLBoolean)
          .description("Whether vehicle may be alighted at quay.")
          .dataFetcher(environment ->
            ((TripTimeOnDate) environment.getSource()).getDropoffType() != PickDrop.NONE
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("requestStop")
          .type(Scalars.GraphQLBoolean)
          .description("Whether vehicle will only stop on request.")
          .dataFetcher(environment ->
            ((TripTimeOnDate) environment.getSource()).getDropoffType() ==
            PickDrop.COORDINATE_WITH_DRIVER
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("serviceJourney")
          .type(serviceJourneyType)
          .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).getTrip())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("destinationDisplay")
          .type(destinationDisplayType)
          .dataFetcher(DataFetchingEnvironment::getSource)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("notices")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(noticeType))))
          .dataFetcher(environment -> {
            TripTimeOnDate tripTimeOnDate = environment.getSource();
            return GqlUtil
              .getTransitService(environment)
              .getNoticesByEntity(tripTimeOnDate.getStopTimeKey());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("bookingArrangements")
          .description("Booking arrangements for this passing time.")
          .type(bookingArrangementType)
          .dataFetcher(environment -> environment.<TripTimeOnDate>getSource().getPickupBookingInfo()
          )
          .build()
      )
      .build();
  }

  /**
   * Generally the missing values are removed during the graph build. However, for flex trips they
   * are not and have to be converted to null here.
   */
  private static Integer missingValueToNull(int value) {
    if (value == StopTime.MISSING_VALUE) {
      return null;
    } else {
      return value;
    }
  }
}
