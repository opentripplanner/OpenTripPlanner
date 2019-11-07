package org.opentripplanner.graph_builder.annotation;

public class QuayWithoutCoordinates extends GraphBuilderAnnotation {

    public static final String FMT = "Quay %s does not contain any coordinates.";

    final String quayId;

    public QuayWithoutCoordinates(String quayId) {
        this.quayId = quayId;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, quayId);
    }
}
