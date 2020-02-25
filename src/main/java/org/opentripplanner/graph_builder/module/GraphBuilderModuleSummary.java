package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to store statistics about various how various graph build modules performed.
 */
public class GraphBuilderModuleSummary extends GraphBuilderSummary {
    private List<GraphBuilderTaskSummary> subTasks = new ArrayList<>();

    public GraphBuilderModuleSummary(GraphBuilderModule module) {
        this.name = module.getClass().getSimpleName();
    }

    @Override
    public String getLogDisplayName() {
        return String.format("graph builder module: %s", name);
    }

    public GraphBuilderTaskSummary addSubTask(String name) {
        GraphBuilderTaskSummary subTask = new GraphBuilderTaskSummary(this, name);
        subTasks.add(subTask);
        return subTask;
    }

    public List<GraphBuilderTaskSummary> getSubTasks() {
        return subTasks;
    }
}
