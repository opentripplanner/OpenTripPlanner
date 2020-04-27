package org.opentripplanner.transit.raptor.rangeraptor.path.configure;


import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.transit.raptor.rangeraptor.transit.SearchContext;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoComparator;

import static org.opentripplanner.transit.raptor.rangeraptor.path.PathParetoSetComparators.comparatorStandard;
import static org.opentripplanner.transit.raptor.rangeraptor.path.PathParetoSetComparators.comparatorWithCost;
import static org.opentripplanner.transit.raptor.rangeraptor.path.PathParetoSetComparators.comparatorWithRelaxedCost;
import static org.opentripplanner.transit.raptor.rangeraptor.path.PathParetoSetComparators.comparatorWithTimetable;
import static org.opentripplanner.transit.raptor.rangeraptor.path.PathParetoSetComparators.comparatorWithTimetableAndCost;
import static org.opentripplanner.transit.raptor.rangeraptor.path.PathParetoSetComparators.comparatorWithTimetableAndRelaxedCost;

/**
 * This class is responsible for creating a a result collector - the
 * set of paths.
 * <p/>
 * This class have REQUEST scope, so a new instance should be created
 * for each new request/travel search.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class PathConfig<T extends RaptorTripSchedule> {
    private final SearchContext<T> ctx;

    public PathConfig(SearchContext<T> context) {
        this.ctx = context;
    }

    /**
     * Create a new {@link DestinationArrivalPaths} each time it is invoked.
     * The given {@code includeCost} decide if the cost should be included in the
     * pareto set criteria or not.
     */
    public DestinationArrivalPaths<T> createDestArrivalPaths(boolean includeCost) {
        return new DestinationArrivalPaths<>(
                paretoComparator(includeCost),
                ctx.calculator(),
                ctx.costCalculator(),
                ctx.pathMapper(),
                ctx.debugFactory(),
                ctx.lifeCycle()
        );
    }

    private ParetoComparator<Path<T>> paretoComparator(boolean includeCost) {
        double relaxedCost = ctx.searchParams().relaxCostAtDestination();
        boolean includeRelaxedCost = includeCost && relaxedCost > 0.0;
        boolean includeTimetable = ctx.searchParams().timetableEnabled();


        if(includeTimetable && includeRelaxedCost) {
            return comparatorWithTimetableAndRelaxedCost(relaxedCost);
        }
        if(includeTimetable && includeCost) {
            return comparatorWithTimetableAndCost();
        }
        if(includeTimetable) {
            return comparatorWithTimetable();
        }
        if(includeRelaxedCost) {
            return comparatorWithRelaxedCost(relaxedCost);
        }
        if(includeCost) {
            return comparatorWithCost();
        }
        return comparatorStandard();
    }
}
