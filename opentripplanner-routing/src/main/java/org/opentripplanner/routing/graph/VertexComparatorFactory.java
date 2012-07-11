package org.opentripplanner.routing.graph;

import java.util.Comparator;
import java.util.List;

public interface VertexComparatorFactory {

	Comparator<? super Vertex> getComparator(List<Vertex> vertexById);

}
