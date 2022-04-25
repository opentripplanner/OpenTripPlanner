package org.opentripplanner.graph_builder.module.osm.contract;

import org.openstreetmap.osmosis.osmbinary.file.BlockReaderAdapter;
import org.opentripplanner.graph_builder.module.osm.OSMParserPhase;

public interface OSMParser extends BlockReaderAdapter {
  void setPhase(OSMParserPhase phase);
}
