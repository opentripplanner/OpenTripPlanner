package org.opentripplanner.ext.restapi.model;

import jakarta.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.opentripplanner.api.error.PlannerError;
import org.opentripplanner.api.resource.DebugOutput;

/** Represents a trip planner response, will be serialized into XML or JSON by Jersey */
public class TripPlannerResponse {

  /** A dictionary of the parameters provided in the request that triggered this response. */
  public HashMap<String, String> requestParameters;
  private ApiTripPlan plan;
  private ApiTripSearchMetadata metadata;
  private String previousPageCursor;
  private String nextPageCursor;

  private PlannerError error = null;

  /** Debugging and profiling information */
  public DebugOutput debugOutput = null;

  public ElevationMetadata elevationMetadata = null;

  /** This no-arg constructor exists to make JAX-RS happy. */
  @SuppressWarnings("unused")
  private TripPlannerResponse() {}

  /** Construct an new response initialized with all the incoming query parameters. */
  public TripPlannerResponse(UriInfo info) {
    this.requestParameters = new HashMap<>();
    if (info == null) {
      // in tests where there is no HTTP request, just leave the map empty
      return;
    }
    for (Entry<String, List<String>> e : info.getQueryParameters().entrySet()) {
      // include only the first instance of each query parameter
      requestParameters.put(e.getKey(), e.getValue().get(0));
    }
  }

  // NOTE: the order the getter methods below is semi-important, in that Jersey will use the
  // same order for the elements in the JS or XML serialized response. The traditional order
  // is request params, followed by plan, followed by errors.

  /** The actual trip plan. */
  public ApiTripPlan getPlan() {
    return plan;
  }

  public void setPlan(ApiTripPlan plan) {
    this.plan = plan;
  }

  /**
   * Use the cursor to get the previous page of results. Insert the cursor into the request and post
   * it to get the previous page.
   * <p>
   * The previous page is a set of itineraries departing BEFORE the first itinerary in the result
   * for a depart after search. When using the default sort order the previous set of itineraries is
   * inserted before the current result.
   * <p>
   * Note! The behavior is undefined if timetableView is off. This is possible to support, but
   * require more information to be included in the cursor.
   */
  public String getPreviousPageCursor() {
    return previousPageCursor;
  }

  public void setPreviousPageCursor(String pageCursor) {
    this.previousPageCursor = pageCursor;
  }

  /**
   * Use the cursor to get the next page of results. Insert the cursor into the request and post it
   * to get the next page.
   * <p>
   * The next page is a set of itineraries departing AFTER the last itinerary in this result.
   * <p>
   * Note! The behavior is undefined if timetableView is off. This is possible to support, but
   * require more information to be included in the cursor.
   */
  public String getNextPageCursor() {
    return nextPageCursor;
  }

  public void setNextPageCursor(String pageCursor) {
    this.nextPageCursor = pageCursor;
  }

  /** The error (if any) that this response raised. */
  public PlannerError getError() {
    return error;
  }

  public void setError(PlannerError error) {
    this.error = error;
  }

  public ApiTripSearchMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(ApiTripSearchMetadata metadata) {
    this.metadata = metadata;
  }
}
