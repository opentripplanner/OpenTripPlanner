package org.opentripplanner.graph_builder.module;

public class GraphBuilderTaskSummary extends GraphBuilderSummary {
    private final GraphBuilderModuleSummary parentModuleSummary;

    public GraphBuilderTaskSummary(GraphBuilderModuleSummary graphBuilderModuleSummary, String name) {
        this.name = name;
        this.parentModuleSummary = graphBuilderModuleSummary;
    }

    @Override
    public String getLogDisplayName() {
        return String.format("%s subtask: %s", parentModuleSummary.getLogDisplayName(), name);
    }
}
