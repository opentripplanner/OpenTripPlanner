package org.opentripplanner.graph_builder.module;

public abstract class GraphBuilderSummary {
    private long endTime;
    protected String name;
    private long startTime = 0;

    public long getDuration() {
        return endTime - startTime;
    }

    public String getName() { return name; }

    public abstract String getLogDisplayName();

    public String finish() {
        if (this.startTime == 0) {
            throw new IllegalStateException("GraphBuilderSummary instance finished before it was started!");
        }
        this.endTime = System.currentTimeMillis();
        return String.format(
            "Finished %s in %.1f seconds",
            getLogDisplayName(),
            getDuration() / 1000.0
        );
    }

    public String start() {
        if (this.startTime != 0) {
            throw new IllegalStateException("GraphBuilderSummary instance has already been started!");
        }
        this.startTime = System.currentTimeMillis();
        return String.format("Starting %s", getLogDisplayName());
    }
}
