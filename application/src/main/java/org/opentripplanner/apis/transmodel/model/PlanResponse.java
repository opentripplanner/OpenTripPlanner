package org.opentripplanner.apis.transmodel.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.TripSearchMetadata;

public class PlanResponse {

  private final TripPlan plan;

  private final TripSearchMetadata metadata;
  private final List<RoutingError> messages;
  private final DebugOutput debugOutput;
  private final PageCursor previousPageCursor;
  private final PageCursor nextPageCursor;

  PlanResponse(
    TripPlan plan,
    @Nullable TripSearchMetadata metadata,
    @Nullable List<RoutingError> messages,
    @Nullable DebugOutput debugOutput,
    @Nullable PageCursor previousPageCursor,
    @Nullable PageCursor nextPageCursor
  ) {
    this.plan = Objects.requireNonNull(plan);
    this.metadata = metadata;
    this.messages = messages != null ? messages : new ArrayList<>();
    this.debugOutput = debugOutput;
    this.previousPageCursor = previousPageCursor;
    this.nextPageCursor = nextPageCursor;
  }

  public static PlanResponseBuilder of() {
    return new PlanResponseBuilder();
  }

  @Nullable
  public DebugOutput debugOutput() {
    return debugOutput;
  }

  public List<RoutingError> messages() {
    return messages;
  }

  @Nullable
  public TripSearchMetadata metadata() {
    return metadata;
  }

  @Nullable
  public PageCursor nextPageCursor() {
    return nextPageCursor;
  }

  @Nullable
  public PageCursor previousPageCursor() {
    return previousPageCursor;
  }

  @Override
  public String toString() {
    return (
      "PlanResponse{" +
      "plan=" +
      plan +
      ", metadata=" +
      metadata +
      ", messages=" +
      messages +
      ", debugOutput=" +
      debugOutput +
      ", previousPageCursor=" +
      previousPageCursor +
      ", nextPageCursor=" +
      nextPageCursor +
      '}'
    );
  }

  public Instant date() {
    return plan.date;
  }

  public Place from() {
    return plan.from;
  }

  public Place to() {
    return plan.to;
  }

  public List<Itinerary> itineraries() {
    return plan.itineraries;
  }

  public static class PlanResponseBuilder {

    private TripPlan plan;
    private TripSearchMetadata metadata;
    private List<RoutingError> messages = new ArrayList<>();
    private DebugOutput debugOutput;
    private PageCursor previousPageCursor;
    private PageCursor nextPageCursor;

    PlanResponseBuilder() {}

    public PlanResponseBuilder withPlan(TripPlan plan) {
      this.plan = plan;
      return this;
    }

    public PlanResponseBuilder withMetadata(@Nullable TripSearchMetadata metadata) {
      this.metadata = metadata;
      return this;
    }

    public PlanResponseBuilder withMessages(List<RoutingError> messages) {
      this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
      return this;
    }

    public PlanResponseBuilder withDebugOutput(@Nullable DebugOutput debugOutput) {
      this.debugOutput = debugOutput;
      return this;
    }

    public PlanResponseBuilder withPreviousPageCursor(@Nullable PageCursor previousPageCursor) {
      this.previousPageCursor = previousPageCursor;
      return this;
    }

    public PlanResponseBuilder withNextPageCursor(@Nullable PageCursor nextPageCursor) {
      this.nextPageCursor = nextPageCursor;
      return this;
    }

    public PlanResponse build() {
      return new PlanResponse(
        this.plan,
        this.metadata,
        this.messages,
        this.debugOutput,
        this.previousPageCursor,
        this.nextPageCursor
      );
    }
  }
}
