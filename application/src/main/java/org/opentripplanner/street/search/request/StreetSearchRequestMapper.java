package org.opentripplanner.street.search.request;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.routing.api.request.RouteRequest;

public class StreetSearchRequestMapper {

  public static StreetSearchRequestBuilder map(RouteRequest request) {
    var time = request.dateTime() == null ? RouteRequest.normalizeNow() : request.dateTime();
    StreetSearchRequestBuilder streetSearchRequestBuilder = StreetSearchRequest.of()
      .withStartTime(time)
      .withPreferences(request.preferences())
      .withWheelchair(request.journey().wheelchair())
      .withFrom(request.from())
      .withTo(request.to());

    Duration rentalDuration = request.journey().direct().rentalDuration();
    if (rentalDuration != null) {
      Instant rentalStart = request.arriveBy() ? time.minus(rentalDuration) : time;
      Instant rentalEnd = request.arriveBy() ? time : time.plus(rentalDuration);
      RentalPeriod rentalPeriod = new RentalPeriodBuilder()
        .setRentalStartTime(rentalStart)
        .setRentalEndTime(rentalEnd)
        .build();
      return streetSearchRequestBuilder.withRentalPeriod(rentalPeriod);
    }

    return streetSearchRequestBuilder;
  }

  public static StreetSearchRequestBuilder mapToTransferRequest(RouteRequest request) {
    return StreetSearchRequest.of()
      .withStartTime(Instant.ofEpochSecond(0))
      .withPreferences(request.preferences())
      .withWheelchair(request.journey().wheelchair())
      .withMode(request.journey().transfer().mode());
  }
}
