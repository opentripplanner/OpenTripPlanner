package org.opentripplanner.routing.impl;

import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.services.GraphService;

/**
 * This simple implementation of {@link GraphService} is mostly useful for testing
 * 
 * @author bdferris
 * @see GraphServiceImpl
 * @see GraphService
 */
public class GraphServiceBeanImpl implements GraphService {

    private Graph graph;

    private ContractionHierarchySet contractionHierarchySet;

    private CalendarService calendarService;

    public GraphServiceBeanImpl() {

    }

    public GraphServiceBeanImpl(Graph graph) {
        this.graph = graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public void setContractionHierarchySet(ContractionHierarchySet contractionHierarchySet) {
        this.contractionHierarchySet = contractionHierarchySet;
    }

    public void setCalendarService(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /****
     * {@link GraphService} Interface
     ****/

    @Override
    public void refreshGraph() {

    }

    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public ContractionHierarchySet getContractionHierarchySet() {
        return contractionHierarchySet;
    }

    @Override
    public CalendarService getCalendarService() {
        return calendarService;
    }
}
