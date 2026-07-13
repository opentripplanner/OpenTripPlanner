package org.opentripplanner.inspector.vector.transfers;

import static org.opentripplanner.inspector.vector.KeyValue.kv;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.transfer.regular.model.PathTransfer;

public class PathTransferPropertyMapper extends PropertyMapper<PathTransfer> {

  @Override
  protected Collection<KeyValue> map(PathTransfer input) {
    return List.of(
      kv("class", input.getClass().getSimpleName()),
      kv("name", "Flex transfer %s → %s".formatted(input.from.getId(), input.to.getId())),
      kv("from", input.from.getId()),
      kv("to", input.to.getId()),
      kv("meters", input.getDistanceMeters())
    );
  }
}
