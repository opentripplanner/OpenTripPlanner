package org.opentripplanner.analyst.scenario;

import com.beust.jcommander.internal.Lists;

import java.util.List;

/**
 * A scenario is an ordered sequence of modifications that will be applied non-destructively on top of a baseline graph.
 */
public class Scenario {

    public String description;

    public final List<Modification> modifications = Lists.newArrayList();

    public Scenario (String description) {
        this.description = description;
    }

    public void applyToGraph() {
        for (Modification modification : modifications) {
            modification.applyToGraph();
        }
    }
}
