package org.opentripplanner.routing.api.request.refactor.request;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.pagecursor.PageCursor;

public class RouteRequest {
  Instant dateTime;
  GenericLocation from;
  GenericLocation to;
  Duration searchWindow;
  PageCursor pageCursor;
  boolean timetableView;
  boolean arriveBy = false;
  Locale locale = new Locale("en", "US");
  int numItineraries = 50;
}
