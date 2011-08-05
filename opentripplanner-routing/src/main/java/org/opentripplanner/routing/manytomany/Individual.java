package org.opentripplanner.routing.manytomany;

import org.opentripplanner.routing.core.Vertex;

/**
 * Individual locations that make up Populations for the purpose
 * of many-to-many searches.
 *  
 * @author andrewbyrd
 *
 */
public class Individual {
	double data;
	double result;
	Vertex vertex;
	
	public Individual(Vertex vertex, double data) {
		this.data = data;
		this.vertex = vertex;
		this.result = Double.POSITIVE_INFINITY;
	}
}
