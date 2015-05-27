package org.opentripplanner.analyst.scenario;

import com.beust.jcommander.internal.Lists;

import java.io.Serializable;
import java.util.List;

/**
 * A scenario is an ordered sequence of modifications that will be applied non-destructively on top of a baseline graph.
 */
public class Scenario implements Serializable {

    public final int id;

    public String description = "no description provided";

    public List<Modification> modifications = Lists.newArrayList();

    public Scenario (int id) {
        this.id = id;
    }
}
