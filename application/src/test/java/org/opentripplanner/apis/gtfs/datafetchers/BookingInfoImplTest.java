package org.opentripplanner.apis.gtfs.datafetchers;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

class BookingInfoImplTest {

    @Test
    void map() throws Exception {

      ExecutionInput executionInput = ExecutionInput
        .newExecutionInput()
        .query("")
        .operationName("plan")
        .locale(Locale.ENGLISH)
        .build();

      var executionContext = newExecutionContextBuilder()
        .executionInput(executionInput)
        .executionId(ExecutionId.from(this.getClass().getName()))
        .build();
      var env = DataFetchingEnvironmentImpl
        .newDataFetchingEnvironment(executionContext)
        .arguments(Map.of())
        .source(BookingInfo.of().withMinimumBookingNotice(Duration.ofMinutes(10)).build())
        .build();
      var impl = new BookingInfoImpl();
      var seconds = impl.minimumBookingNoticeSeconds().get(env);
      assertEquals(600, seconds);
    }
  
  

}