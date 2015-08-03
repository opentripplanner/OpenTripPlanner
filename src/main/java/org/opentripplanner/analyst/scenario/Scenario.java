package org.opentripplanner.analyst.scenario;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * A scenario is an ordered sequence of modifications that will be applied non-destructively on top of a baseline graph.
 */
public class Scenario implements Serializable {

    public final int id;

    public String description = "no description provided";

    public List<Modification> modifications = Lists.newArrayList();

    public Scenario (@JsonProperty("id") int id) {
        this.id = id;
    }
}
