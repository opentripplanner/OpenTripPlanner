package org.opentripplanner.ext.flex.trip;

import org.apache.commons.lang3.ArrayUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.ContinuousStopsFlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.graph_builder.module.map.StreetMatcher;
import org.opentripplanner.model.*;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.vertextype.StreetVertex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import static org.opentripplanner.model.StopLocation.expandStops;
import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;


public class ContinuousPickupDropOffTrip extends FlexTrip<Double> {

  private final ContinuousPickupDropOffStopTime[] stopTimes;
  private final FlexStopLocation[] continuousStops;
  private Vertex[] vertices;
  private double[] stopIndices;
  private double[] distances;
  private final BookingInfo[] bookingInfos;

  public ContinuousPickupDropOffTrip(Trip trip, List<StopTime> stopTimes) {
    super(trip);

    if (!hasContinuousStops(stopTimes)) {
      throw new IllegalArgumentException("Incompatible stopTimes for continuous stops flex trip");
    }

    int nStops = stopTimes.size();
    this.stopTimes = new ContinuousPickupDropOffStopTime[nStops];
    this.continuousStops = new FlexStopLocation[nStops - 1];
    this.bookingInfos = new BookingInfo[nStops];

    for (int i = 0; i < nStops; i++) {
      this.stopTimes[i] = new ContinuousPickupDropOffStopTime(stopTimes.get(i));
      this.bookingInfos[i] = stopTimes.get(i).getBookingInfo();
    }
  }

  public static boolean hasContinuousStops(List<StopTime> stopTimes) {
    return stopTimes
        .stream()
        .anyMatch(st -> st.getFlexContinuousPickup() != PICKDROP_NONE || st.getFlexContinuousDropOff() != PICKDROP_NONE);
  }

  @Override
  public Stream<FlexAccessTemplate<Double>> getFlexAccessTemplates(
      NearbyStop access, FlexServiceDate date, FlexPathCalculator<Integer> calculator
  ) {
    int stopArrayIndex = ArrayUtils.indexOf(vertices, access.state.getVertex());
    if (stopArrayIndex == -1) { return Stream.empty(); }
    double fromIndex = stopIndices[stopArrayIndex];

    ArrayList<FlexAccessTemplate<Double>> res = new ArrayList<>();

    ContinuousStopsFlexPathCalculator continuousCalculator = new ContinuousStopsFlexPathCalculator(this);

    boolean isFromStop = Math.rint(fromIndex) == fromIndex;

    // This would return only trips which can be found by raptor
    if (isFromStop) { return Stream.empty(); }

    for (int toIndex = (int) Math.ceil(fromIndex); toIndex < stopTimes.length; toIndex++) {
      if (stopTimes[toIndex].dropOffType == PICKDROP_NONE) continue;
      for (StopLocation stop : expandStops(stopTimes[toIndex].stop)) {
        res.add(new FlexAccessTemplate<>(access, this, fromIndex, (double) toIndex, stop, date, continuousCalculator));
      }
    }

    return res.stream();
  }

  @Override
  public Stream<FlexEgressTemplate<Double>> getFlexEgressTemplates(
      NearbyStop egress, FlexServiceDate date, FlexPathCalculator<Integer> calculator
  ) {
    int stopArrayIndex = ArrayUtils.indexOf(vertices, egress.state.getVertex());
    if (stopArrayIndex == -1) { return Stream.empty(); }
    double toIndex = stopIndices[stopArrayIndex];

    ArrayList<FlexEgressTemplate<Double>> res = new ArrayList<>();

    ContinuousStopsFlexPathCalculator continuousCalculator = new ContinuousStopsFlexPathCalculator(this);

    boolean isToStop = Math.rint(toIndex) == toIndex;

    // This would return only trips which can be found by raptor
    if (isToStop) { return Stream.empty(); }

    for (int fromIndex = (int) Math.floor(toIndex); fromIndex >= 0; fromIndex--) {
      if (stopTimes[fromIndex].pickupType == PICKDROP_NONE) continue;
      for (StopLocation stop : expandStops(stopTimes[fromIndex].stop)) {
        res.add(new FlexEgressTemplate<>(egress, this, (double) fromIndex, toIndex, stop, date, continuousCalculator));
      }
    }

    return res.stream();
  }

  @Override
  public int earliestDepartureTime(
      int departureTime, Double fromStopIndex, Double toStopIndex, int flexTime
  ) {
    if (Math.rint(fromStopIndex) == fromStopIndex) {
      return stopTimes[(int) (double) fromStopIndex].departureTime;
    }
    int stopTime = getStopTime(fromStopIndex);
    return stopTime >= departureTime ? stopTime : -1;
  }

  @Override
  public int latestArrivalTime(
      int arrivalTime, Double fromStopIndex, Double toStopIndex, int flexTime
  ) {
    if (Math.rint(toStopIndex) == toStopIndex) {
      return stopTimes[(int) (double) toStopIndex].arrivalTime;
    }
    int stopTime = getStopTime(toStopIndex);
    return stopTime <= arrivalTime ? stopTime : -1;
  }

  private int getStopTime(Double stopIndex) {
    int prevStop = (int) Math.floor(stopIndex);
    int nextStop = (int) Math.ceil(stopIndex);

    int departureFromPrevious = stopTimes[prevStop].departureTime;
    int arrivalToNext = stopTimes[nextStop].arrivalTime;

    return departureFromPrevious + (int) ((arrivalToNext - departureFromPrevious) * (stopIndex - prevStop));
  }

  @Override
  public Collection<StopLocation> getStops() {
    List<StopLocation> stops = new ArrayList<>(Arrays.asList(continuousStops));
    Arrays.stream(stopTimes).forEach(stopTime -> stops.add(stopTime.stop));

    return stops;
  }

  @Override
  public BookingInfo getBookingInfo(int i) {
    return bookingInfos[i];
  }

  public void addGeometries(
      Graph graph, StreetMatcher matcher
  ) {
    TripPattern pattern = graph.index.getPatternForTrip().get(trip);

    if (pattern == null || pattern.getGeometry() == null) { return; }

    FeedScopedId tripId = trip.getId();

    ArrayList<Vertex> tempVertices = new ArrayList<>();
    ArrayList<Double> tempStopIndices = new ArrayList<>();
    ArrayList<Double> tempDistances = new ArrayList<>();

    double cumulativeDistance = 0;

    for (int i = 0; i < pattern.numHopGeometries(); i++) {
      ContinuousPickupDropOffStopTime stopTime = stopTimes[i];
      if (stopTime.continuousPickupType == PICKDROP_NONE
          && stopTime.continuousDropOffType == PICKDROP_NONE) {
        continue;
      }

      List<StreetEdge> edges = matcher.match(pattern.getHopGeometry(i));
      if (edges == null || edges.isEmpty()) { continue; }

      StopLocation stop = stopTime.stop;
      // TODO: How to generate id?
      String id = tripId.getId() + "_" + stop.getId().getId();
      FeedScopedId feedScopedId = new FeedScopedId(tripId.getFeedId(), id);
      var location = new FlexStopLocation(feedScopedId);
      location.setName(stop.getName() + " -> " + stopTimes[i + 1].stop.getName());
      continuousStops[i] = location;
      graph.locationsById.put(feedScopedId, location);

      int size = edges.size();

      List<Coordinate> coordinates = new ArrayList<>();
      double[] times = new double[size];
      double cumulativeTime = 0;
      tempDistances.add(cumulativeDistance);

      for (int j = 0; j < size; j++) {
        StreetEdge e = edges.get(j);
        coordinates.addAll(Arrays.asList(e.getGeometry().getCoordinates()));
        double distanceMeters = e.getDistanceMeters();
        cumulativeDistance += distanceMeters;
        tempDistances.add(cumulativeDistance);
        double time = distanceMeters / e.getCarSpeed();
        cumulativeTime += time;
        times[j] = cumulativeTime;
      }

      Vertex vertex = edges.get(0).getFromVertex();
      StreetVertex sv;
      if (vertex instanceof StreetVertex) {
        sv = (StreetVertex) vertex;
        if (sv.flexStopLocations == null) {
          sv.flexStopLocations = new HashSet<>();
        }
        sv.flexStopLocations.add(location);
      }

      tempVertices.add(vertex);
      tempStopIndices.add((double) i);

      for (int j = 0; j < size; j++) {
        vertex = edges.get(j).getToVertex();
        if (vertex instanceof StreetVertex) {
          sv = (StreetVertex) vertex;
          if (sv.flexStopLocations == null) {
            sv.flexStopLocations = new HashSet<>();
          }
          sv.flexStopLocations.add(location);
        }
        tempVertices.add(vertex);
        tempStopIndices.add(i + (times[j] / cumulativeTime));
      }

      Coordinate[] coordinateArray = new Coordinate[coordinates.size()];
      LineString ls = GeometryUtils.getGeometryFactory().createLineString(coordinates.toArray(coordinateArray));
      location.setGeometry(ls);
    }

    this.vertices = tempVertices.toArray(new Vertex[0]);
    this.stopIndices = tempStopIndices.stream().mapToDouble(i -> i).toArray();
    this.distances = tempDistances.stream().mapToDouble(i -> i).toArray();
  }

  private static class ContinuousPickupDropOffStopTime implements Serializable {
    private final StopLocation stop;
    private final int departureTime;
    private final int arrivalTime;
    private final int pickupType;
    private final int dropOffType;
    private final int continuousPickupType;
    private final int continuousDropOffType;


    private ContinuousPickupDropOffStopTime(StopTime st) {
      this.stop = st.getStop();

      this.arrivalTime = st.getArrivalTime();
      this.departureTime = st.getDepartureTime();

      this.pickupType = st.getPickupType();
      this.dropOffType = st.getDropOffType();

      this.continuousPickupType = st.getFlexContinuousPickup();
      this.continuousDropOffType = st.getFlexContinuousDropOff();
    }
  }
}
