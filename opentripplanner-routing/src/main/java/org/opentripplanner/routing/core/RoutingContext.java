package org.opentripplanner.routing.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.algorithm.strategies.DefaultExtraEdgesStrategy;
import org.opentripplanner.routing.algorithm.strategies.DefaultRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.ExtraEdgesStrategy;
import org.opentripplanner.routing.algorithm.strategies.GenericAStarFactory;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultRemainingWeightHeuristicFactoryImpl;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RoutingContext holds information needed to carry out a search for a particular TraverseOptions,
 * on a specific graph. Includes things like (temporary) endpoint vertices, transfer tables, 
 * service day caches, etc.
 * 
 * @author abyrd
 */
public class RoutingContext {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingContext.class);
    
    private static GraphService graphService = new GraphServiceImpl(); 
    private static RemainingWeightHeuristicFactory heuristicFactory = new DefaultRemainingWeightHeuristicFactoryImpl();
    
    private TraverseOptions opt;
    public final Graph graph;
    public final Vertex fromVertex;
    public final Vertex toVertex;
    public final Vertex origin;
    public final Vertex target;
    public final State initialState;
    public final boolean goalDirection = true;
    public final StreetVertexIndexService streetIndex;
    //public final Calendar calendar;
    public final CalendarService calendarService;
    public final Map<AgencyAndId, Set<ServiceDate>> serviceDatesByServiceId = new HashMap<AgencyAndId, Set<ServiceDate>>();
    public final GenericAStarFactory aStarSearchFactory = null;
    public final RemainingWeightHeuristic remainingWeightHeuristic;
    public final ExtraEdgesStrategy extraEdgesStrategy = new DefaultExtraEdgesStrategy();
    public final TransferTable transferTable;
    
    /**
     * Cache lists of which transit services run on which midnight-to-midnight periods This ties a
     * TraverseOptions to a particular start time for the duration of a search so the same options
     * cannot be used for multiple searches concurrently. To do so this cache would need to be moved
     * into StateData, with all that entails.
     */
    public ArrayList<ServiceDay> serviceDays;

    public RoutingContext(TraverseOptions traverseOptions) {
        this(traverseOptions, true);
    }

    /** return the vertex where this search will begin, accounting for arriveBy */
    public Vertex getOriginVertex() {
        return opt.arriveBy ? toVertex : fromVertex;
    }
    
    /** return the vertex where this search will end, accounting for arriveBy */
    public Vertex getTargetVertex() {
        return opt.arriveBy ? fromVertex : toVertex;
    }
    
    public boolean serviceOn(AgencyAndId serviceId, ServiceDate serviceDate) {
        Set<ServiceDate> dates = serviceDatesByServiceId.get(serviceId);
        if (dates == null) {
            dates = calendarService.getServiceDatesForServiceId(serviceId);
            serviceDatesByServiceId.put(serviceId, dates);
        }
        return dates.contains(serviceDate);
    }

    public RoutingContext(TraverseOptions traverseOptions, boolean useServiceDays) {
        graph = graphService.getGraph(); // opt.routerId
        opt.graph = graph; // TODO: add routingcontext to SPT
        streetIndex = graph.getService(StreetVertexIndexService.class);
        fromVertex = streetIndex.getVertexForPlace(opt.getFromPlace(), opt);
        toVertex = streetIndex.getVertexForPlace(opt.getToPlace(), opt, fromVertex);
        // state.reversedClone() will have to set the vertex, not get it from opts
        origin = opt.arriveBy ? toVertex : fromVertex;
        target = opt.arriveBy ? fromVertex : toVertex;
        initialState = new State(origin, opt);
        checkEndpointVertices();
        findIntermediateVertices();
//        CalendarServiceData csData = graph.getService(CalendarServiceData.class);
//        if (csData != null) {
//            calendarService = new CalendarServiceImpl();
//            calendarService.setData(csData);
//        }
        calendarService = graphService.getCalendarService();
        transferTable = graph.getTransferTable();
        if (useServiceDays)
            setServiceDays();
        remainingWeightHeuristic = heuristicFactory.getInstanceForSearch(opt);
        if (opt.getModes().isTransit()
            && ! graph.transitFeedCovers(opt.dateTime)) {
            // user wants a path through the transit network,
            // but the date provided is outside those covered by the transit feed.
            throw new TransitTimesException();
        }
    }
    
    private void checkEndpointVertices() {
        ArrayList<String> notFound = new ArrayList<String>();
        if (fromVertex == null)
            notFound.add("from");
        if (toVertex == null)
            notFound.add("to");
        if (notFound.size() > 0)
            throw new VertexNotFoundException(notFound);
    }
    
    private void findIntermediateVertices() {
        if (opt.intermediatePlaces == null)
            return;
        ArrayList<String> notFound = new ArrayList<String>();
        ArrayList<Vertex> intermediateVertices = new ArrayList<Vertex>();
        int i = 0;
        for (NamedPlace intermediate : opt.intermediatePlaces) {
            Vertex vertex = streetIndex.getVertexForPlace(intermediate, opt);
            if (vertex == null) {
                notFound.add("intermediate." + i);
            } else {
                intermediateVertices.add(vertex);
            }
            i += 1;
        }
        if (notFound.size() > 0) {
            throw new VertexNotFoundException(notFound);
        }        
    }

    /**
     *  Cache ServiceDay objects representing which services are running yesterday, today, and tomorrow relative
     *  to the search time. This information is very heavily used (at every transit boarding) and Date operations were
     *  identified as a performance bottleneck. Must be called after the TraverseOptions already has a CalendarService set. 
     */
    public void setServiceDays() {
        final long SEC_IN_DAY = 60 * 60 * 24;
        final long time = opt.getSecondsSinceEpoch();
        this.serviceDays = new ArrayList<ServiceDay>(3);
        if (calendarService == null) {
            LOG.warn("TraverseOptions has no CalendarService or GTFSContext. Transit will never be boarded.");
            return;
        }
        // This should be a valid way to find yesterday and tomorrow,
        // since DST changes more than one hour after midnight in US/EU.
        // But is this true everywhere?
        for (String agency : graph.getAgencyIds()) {
            addIfNotExists(this.serviceDays, new ServiceDay(time - SEC_IN_DAY, calendarService, agency));
            addIfNotExists(this.serviceDays, new ServiceDay(time, calendarService, agency));
            addIfNotExists(this.serviceDays, new ServiceDay(time + SEC_IN_DAY, calendarService, agency));
        }
    }

    private static<T> void addIfNotExists(ArrayList<T> list, T item) {
        if (!list.contains(item)) {
            list.add(item);
        }
    }

    /** check if the start and end locations are accessible */
    public boolean isAccessible() {
        if (opt.getWheelchair()) {
            return isWheelchairAccessible(fromVertex) &&
                   isWheelchairAccessible(toVertex);
        }
        return true;
    }

    // this could be handled by method overloading on Vertex
    public boolean isWheelchairAccessible(Vertex v) {
        if (v instanceof TransitStop) {
            TransitStop ts = (TransitStop) v;
            return ts.hasWheelchairEntrance();
        } else if (v instanceof StreetLocation) {
            StreetLocation sl = (StreetLocation) v;
            return sl.isWheelchairAccessible();
        }
        return true;
    }

    /** 
     * When a routing context is garbage collected, there should be no more references
     * to the temporary vertices it created. We need to detach its edges from the permanent graph.
     */
    @Override public void finalize() {
        LOG.debug("garbage routing context collected");
    }

}
