package org.opentripplanner.graph_builder.annotation;

public class TooManyAreasInRelation extends GraphBuilderAnnotation {

    public static final String FMT = "Too many areas in relation %s";

    final long relationId;

    public TooManyAreasInRelation(long relationId) {
        this.relationId = relationId;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, relationId);
    }
}
