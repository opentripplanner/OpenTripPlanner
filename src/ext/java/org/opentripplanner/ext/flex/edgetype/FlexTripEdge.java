package org.opentripplanner.ext.flex.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessEgressTemplate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import java.util.Locale;
import java.util.TimeZone;

public class FlexTripEdge extends Edge {

  private static final long serialVersionUID = 1L;

  public StopLocation s1;
  public StopLocation s2;
  
  public FlexAccessEgressTemplate flexTemplate;
  public FlexPath flexPath;

  public FlexTripEdge(
      Vertex v1, Vertex v2, StopLocation s1, StopLocation s2,
      FlexAccessEgressTemplate flexTemplate, FlexPathCalculator calculator
  ) {
    // Why is this code so dirty? Because we don't want this edge to be added to the edge lists.
    // The first parameter in Vertex constructor is graph. If it is null, the vertex isn't added to it.
    super(new Vertex(null, null, 0.0, 0.0) {}, new Vertex(null, null, 0.0, 0.0) {});
    
    this.s1 = s1;
    this.s2 = s2;
    this.flexTemplate = flexTemplate;
    this.fromv = v1;
    this.tov = v2;
    this.flexPath = calculator.calculateFlexPath(fromv, tov, s1, s2, 
    		flexTemplate.fromStopIndex, flexTemplate.toStopIndex);
  }

  @Override
  public State traverse(State s0) {
	if(this.flexPath == null)
		return null; // not routable
	  
	StateEditor editor = s0.edit(this);
    editor.setBackMode(TraverseMode.BUS);

    // wait time
    TimeZone tz = s0.getOptions().getRoutingContext().graph.getTimeZone();    
	long serviceDateAsEpoch = flexTemplate.serviceDate.getAsEpochSeconds(tz);
	long pickupTime = (s0.getTimeInMillis()/1000) - serviceDateAsEpoch;
	
	if(pickupTime < 0 || pickupTime > 60 * 60 * 24)
		return null; // current time isn't in this service day
		
	int earliestPickupTime = flexTemplate.getFlexTrip().earliestDepartureTime((int)pickupTime, 
    		flexTemplate.fromStopIndex, flexTemplate.toStopIndex);

	if(earliestPickupTime < 0)
		return null; // trip occurs outside the planning window
	
	long initialWaitTimeSeconds = earliestPickupTime - pickupTime;
	if(initialWaitTimeSeconds < 0)
		return null; // we missed this trip
		
	editor.incrementWeight((int)initialWaitTimeSeconds);
	
	// travel time
    editor.incrementTimeInSeconds(getTripTimeInSeconds());
    editor.incrementWeight(getTripTimeInSeconds());

    editor.resetEnteredMotorVerhicleNoThroughTrafficArea();
    return editor.makeState();
  }

  // This method uses the "mean" time from flex v2 to best reflect the typical travel scenario
  // in user-facing interfaces.
  public int getTripTimeInSeconds() {
    return getFlexTrip().getMeanTotalTime(flexPath, flexTemplate.fromStopIndex, flexTemplate.toStopIndex);
  }

  @Override
  public double getDistanceMeters() {
    return flexPath.distanceMeters;
  }

  @Override
  public LineString getGeometry() {
    return flexPath.geometry;
  }

  @Override
  public Trip getTrip() {
    return getFlexTrip().getTrip();
  }

  public FlexTrip getFlexTrip() {
    return flexTemplate.getFlexTrip();
  }

  @Override
  public String getName() {
    return getTrip().getId().toString();
  }

  @Override
  public String getName(Locale locale) {
 	return getName();
  }
}
