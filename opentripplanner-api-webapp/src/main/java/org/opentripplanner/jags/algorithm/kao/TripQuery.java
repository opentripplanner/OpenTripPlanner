package org.opentripplanner.jags.algorithm.kao;

import java.util.GregorianCalendar;

import org.opentripplanner.jags.core.Vertex;


public class TripQuery {
	public GregorianCalendar t_0;
	public long delta;
	public Vertex v_0;
	public Vertex v_gamma;
	
	public TripQuery(GregorianCalendar t_0, long delta, Vertex v_0, Vertex v_gamma) {
		this.t_0 = t_0;
		this.delta = delta;
		this.v_0 = v_0;
		this.v_gamma = v_gamma;
	}
}