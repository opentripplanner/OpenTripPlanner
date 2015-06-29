package org.opentripplanner.analyst.scenario;

import java.util.HashMap;
import java.util.Map;

/**
 * Retains multiple Scenarios for a single router, keyed on unique IDs.
 */
public class ScenarioStore {

    private int nextId = 0;

    public final Map<String, Scenario> scenarios = new HashMap<>();

    public Map<String, String> getDescriptions() {
        Map<String, String> descriptionForScenarioId = new HashMap<>();
        for (String scenarioId : scenarios.keySet()) {
            descriptionForScenarioId.put(scenarioId, scenarios.get(scenarioId).description);
        }
        return descriptionForScenarioId;
    }

    private synchronized int getNextId() {
        return nextId++;
    }

    public synchronized Scenario getNewEmptyScenario() {
        int id = getNextId();
        Scenario scenario = new Scenario(id);
        scenarios.put(Integer.toString(id), scenario);
        return scenario;
    }

}
