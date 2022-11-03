package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.transit.model.basic.I18NString;

public record VertexAndName(I18NString name, IntersectionVertex vertex) {}
