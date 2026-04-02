package org.opentripplanner.apis.transmodel.model.timetable;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import org.opentripplanner.routing.api.response.TripSearchMetadata;

public class TripMetadataType {

  private TripMetadataType() {}

  public static GraphQLObjectType create(GraphQLScalarType dateTimeScalar) {
    return GraphQLObjectType.newObject()
      .name("TripSearchData")
      .description("Trips search metadata.")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("pageDepartureTimeStart")
          .description(
            """
            The start-time of the search-window/page for trip departure times.

            The search-window/page start and end time describe the time-window the search is
            performed in. All results in the window is expected to be inside the given window. When
            navigating to the next/previous window the new window might overlap.

            **Merging results from multiple searches**

            Trips from separate searches (multiple OTP calls or other search engines) can be merged
            into the current page/result set if the following conditions are met:
            - The page is empty and the candidate trip departure time is between
              `pageDepartureTimeStart` and `pageDepartureTimeEnd`, or
            - The candidate trip sorts before the last trip in the current page(if trips exists).

            If the trip sorts after the last trip, it should be merged into the next page instead.
            Note that the sort order is diffrent for arrive-by and depart-after search. The sort
            vector is:
            - Depart-after: _departure-time_ → _generalized-cost_ → _number-of-transfers_ → _arrival-time_
            - Arrive-by: _arrival-time_ → _generalized-cost_ → _number-of-transfers_ → _departure-time_

            **Special case for arrive-by searches:** For the first request (no paging cursor used)
            with `arriveBy=true`, the `pageDepartureTimeEnd` can be ignored - trips departing after
            this time can still be merged into the current page.
            """
          )
          .type(dateTimeScalar)
          .dataFetcher(e -> ((TripSearchMetadata) e.getSource()).pageDepartureTimeStart())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("pageDepartureTimeEnd")
          .description(
            "The end-time of the search-window/page for trip departure times. See `pageDepartureTimeStart`"
          )
          .type(dateTimeScalar)
          .dataFetcher(e -> ((TripSearchMetadata) e.getSource()).pageDepartureTimeEnd())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("searchWindowUsed")
          .description(
            "This is the time window used by the raptor search. The input searchWindow " +
              "is an optional parameter and is dynamically assigned if not set. OTP might " +
              "override the value if it is too small or too large. When paging OTP adjusts " +
              "it to the appropriate size, depending on the number of itineraries found in " +
              "the current search window. The scaling of the search window ensures faster " +
              "paging and limits resource usage. The unit is minutes."
          )
          .deprecate("This not needed for debugging, and is misleading if the window is cropped.")
          .type(new GraphQLNonNull(Scalars.GraphQLInt))
          .dataFetcher(e ->
            ((TripSearchMetadata) e.getSource()).raptorSearchWindowUsed().toMinutes()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("nextDateTime")
          .description("This will not be available after March 2026!")
          .deprecate("Use pageCursor instead")
          .type(dateTimeScalar)
          .dataFetcher(e -> ((TripSearchMetadata) e.getSource()).nextDateTime())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("prevDateTime")
          .description("This will not be available after March 2026!")
          .deprecate("Use pageCursor instead")
          .type(dateTimeScalar)
          .dataFetcher(e -> ((TripSearchMetadata) e.getSource()).prevDateTime())
          .build()
      )
      .build();
  }
}
