package org.opentripplanner.routing.services;

import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.core.Graph;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service classes that need access to core graph objects like {@link Graph},
 * {@link ContractionHierarchySet} and {@link CalendarService} should access
 * them through this service interface. Instead of injecting a {@link Autowired}
 * dependency on {@link Graph}, instead inject an instace of
 * {@link GraphService} and use the {@link #getGraph()} method to access the
 * graph as neeeed.
 * 
 * Why the level of indirection? The service interface allows use to more easily
 * decouple the deserialization, loading, and management of the underlying graph
 * objects from the classes that need access to the objects. This indirection
 * allows us to dynamically swap in a new graph if underlying data changes, for
 * example.
 * 
 * @author bdferris
 * 
 */
public interface GraphService {

	/**
	 * Refresh the graph. Depending on the underlying implementation, this may
	 * involve reloading the graph from a file.
	 */
	public void refreshGraph();

	/**
	 * 
	 * @return the current graph object
	 */
	public Graph getGraph();

	/**
	 * 
	 * @return the current contraction hiearachy set object
	 */
	public ContractionHierarchySet getContractionHierarchySet();

	/**
	 * 
	 * @return the current calendar service instance, or null if no calendar
	 *         data is loaded
	 */
	public CalendarService getCalendarService();
}
