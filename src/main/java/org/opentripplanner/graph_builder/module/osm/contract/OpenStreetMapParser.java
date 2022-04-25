package org.opentripplanner.graph_builder.module.osm.contract;

import org.openstreetmap.osmosis.osmbinary.file.BlockReaderAdapter;
import org.opentripplanner.graph_builder.module.osm.OsmParserPhase;

public interface OpenStreetMapParser extends BlockReaderAdapter {
  void setPhase(OsmParserPhase phase);
}
