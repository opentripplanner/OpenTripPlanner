package org.opentripplanner.routing.services;

import java.util.Date;
import java.util.List;

import org.opentripplanner.routing.spt.GraphPath;

public interface PathService {
    public List<GraphPath> plan(String fromPlace, String toPlace, Date targetTime, boolean arriveBy);
}
