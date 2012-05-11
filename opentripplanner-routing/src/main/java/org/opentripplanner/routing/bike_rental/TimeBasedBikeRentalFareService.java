package org.opentripplanner.routing.bike_rental;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.services.ChainedFareService;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeBasedBikeRentalFareService implements ChainedFareService, Serializable {

    private static final long serialVersionUID = 5226621661906177942L;

    private static Logger log = LoggerFactory.getLogger(TimeBasedBikeRentalFareService.class);

    private FareService next;

    // Each entry is <max time, cents at that time>; the list is sorted in
    // ascending time order
    private List<P2<Integer>> pricing_by_second;

    private String currency;

    /*
     * A list of <time>,<cents> time can be a number of seconds, or m:s, or h:m:s
     */
    public void setPricing(List<String> pricing) {
        pricing_by_second = new ArrayList<P2<Integer>>();
        for (String pair : pricing) {
            String[] strings = pair.split(",");
            String[] hms = strings[0].split(":");
            int seconds = 0;
            for (String field : hms) {
                seconds *= 60;
                int fieldValue = Integer.parseInt(field);
                seconds += fieldValue;
            }
            pricing_by_second.add((new P2<Integer>(seconds, Integer.parseInt(strings[1]))));
        }
        Collections.sort(pricing_by_second, new Comparator<P2<Integer>>() {
            @Override
            public int compare(P2<Integer> arg0, P2<Integer> arg1) {
                return arg0.getFirst() - arg1.getFirst();
            }

        });
        int seconds = -1;
        int lastCost = 0;
        for (P2<Integer> bracket : pricing_by_second) {
            int maxTime = bracket.getFirst();
            int cost = bracket.getSecond();
            if (maxTime == seconds) {
                throw new RuntimeException("Bike share pricing has two entries for " + maxTime);
            }
            if (cost < lastCost) {
                log.warn("Bike share pricing has pathological pricing; keeping the bike for a "
                        + maxTime + "  is cheaper than keeping it for " + seconds);
            }
            seconds = maxTime;
            lastCost = cost;
        }
    }

    @Override
    public Fare getCost(GraphPath path) {
        int cost = 0;
        long start = -1;

        for (State state : path.states) {
            if (state.getVertex() instanceof BikeRentalStationVertex
                    && state.getBackState().getVertex() instanceof BikeRentalStationVertex) {
                if (start == -1) {
                    start = state.getTime();
                } else {
                    int time_on_bike = (int) (state.getTime() - start);
                    int ride_cost = -1;
                    for (P2<Integer> bracket : pricing_by_second) {
                        int time = bracket.getFirst();
                        if (time_on_bike < time) {
                            ride_cost = bracket.getSecond();
                            break;
                        }
                    }
                    if (ride_cost == -1) {
                        log.warn("Bike rental has no associated pricing (too long?) : "
                                + time_on_bike + " seconds");
                    } else {
                        cost += ride_cost;
                    }
                    start = -1;
                }
            }
        }

        if (cost == 0) {
            return next.getCost(path);
        }

        if (next == null) {
            Fare fare = new Fare();
            fare.addFare(FareType.regular, new WrappedCurrency(Currency.getInstance(currency)),
                    cost);
            return fare;
        }

        Fare fare = next.getCost(path);
        if (fare == null) {
            fare = new Fare();
            fare.addFare(FareType.regular, new WrappedCurrency(Currency.getInstance(currency)),
                    cost);
            return fare;
        }
        fare.addCost(cost);
        return fare;
    }

    @Override
    public void setNextService(FareService service) {
        this.next = service;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

}
