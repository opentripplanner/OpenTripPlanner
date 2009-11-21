package org.opentripplanner.narrative.services;

import java.util.Date;
import java.util.List;

import org.opentripplanner.narrative.model.Narrative;

public interface NarrativeService {
    public List<Narrative> plan(String fromPlace, String toPlace, Date targetTime);
}
