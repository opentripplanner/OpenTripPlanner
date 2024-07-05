package org.opentripplanner.inspector.vector.vertex;

import static org.opentripplanner.inspector.vector.KeyValue.kv;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.Vertex;

public class VertexPropertyMapper extends PropertyMapper<Vertex> {

  @Override
  protected Collection<KeyValue> map(Vertex input) {
    List<KeyValue> baseProps = List.of(
      kv("class", input.getClass().getSimpleName()),
      kv("label", input.getLabel().toString())
    );
    List<KeyValue> properties =
      switch (input) {
        case BarrierVertex v -> List.of(kv("permission", v.getBarrierPermissions().toString()));
        case VehicleRentalPlaceVertex v -> List.of(kv("rentalId", v.getStation().getId()));
        default -> List.of();
      };
    return ListUtils.combine(baseProps, properties);
  }
}
