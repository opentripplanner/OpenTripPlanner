package org.opentripplanner.graph_builder.annotation;

public class UnableToProcessPublicTransportationRelation extends GraphBuilderAnnotation {

    public static final String FMT = "Unable to process public transportation relation %s";

    final long relationId;

    public UnableToProcessPublicTransportationRelation(long relationId) {
        this.relationId = relationId;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, relationId);
    }
}
