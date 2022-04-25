package org.opentripplanner.graph_builder.module.osm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openstreetmap.osmosis.osmbinary.BinaryParser;
import org.openstreetmap.osmosis.osmbinary.Osmformat;
import org.opentripplanner.graph_builder.module.osm.contract.OSMEntityStore;
import org.opentripplanner.graph_builder.module.osm.contract.OSMParser;
import org.opentripplanner.graph_builder.module.osm.model.OSMNode;
import org.opentripplanner.graph_builder.module.osm.model.OSMNodeRef;
import org.opentripplanner.graph_builder.module.osm.model.OSMRelation;
import org.opentripplanner.graph_builder.module.osm.model.OSMRelationMember;
import org.opentripplanner.graph_builder.module.osm.model.OSMTag;
import org.opentripplanner.graph_builder.module.osm.model.OSMWay;

/**
 * Parser for the OpenStreetMap PBF Format.
 *
 * @since 0.4
 */
public class BinaryOSMParser extends BinaryParser implements OSMParser {

  private final OSMEntityStore osmdb;
  private final Map<String, String> stringTable = new HashMap<>();
  private OSMParserPhase parsePhase;

  public BinaryOSMParser(OSMEntityStore osmdb) {
    this.osmdb = osmdb;
  }

  // The strings are already being pulled from a string table in the PBF file,
  // but there appears to be a separate string table per 8k-entry PBF file block.
  // String.intern grinds to a halt on large PBF files (as it did on GTFS import), so
  // we implement our own.
  protected String internalize(String s) {
    return stringTable.computeIfAbsent(s, key -> s);
  }

  @Override
  public void complete() {
    // Jump in circles
  }

  /**
   * Set the phase to be parsed
   */
  @Override
  public void setPhase(OSMParserPhase phase) {
    this.parsePhase = phase;
  }

  @Override
  protected void parseRelations(List<Osmformat.Relation> rels) {
    if (parsePhase != OSMParserPhase.RELATIONS) {
      return;
    }

    for (Osmformat.Relation i : rels) {
      OSMRelation tmp = new OSMRelation();
      tmp.setId(i.getId());

      for (int j = 0; j < i.getKeysCount(); j++) {
        OSMTag tag = new OSMTag();
        String key = internalize(getStringById(i.getKeys(j)));
        String value = internalize(getStringById(i.getVals(j)));
        tag.setK(key);
        tag.setV(value);
        tmp.addTag(tag);
      }

      long lastMid = 0;
      for (int j = 0; j < i.getMemidsCount(); j++) {
        OSMRelationMember relMember = new OSMRelationMember();
        long mid = lastMid + i.getMemids(j);

        relMember.setRef(mid);
        lastMid = mid;

        relMember.setRole(internalize(getStringById(i.getRolesSid(j))));

        if (i.getTypes(j) == Osmformat.Relation.MemberType.NODE) {
          relMember.setType("node");
        } else if (i.getTypes(j) == Osmformat.Relation.MemberType.WAY) {
          relMember.setType("way");
        } else if (i.getTypes(j) == Osmformat.Relation.MemberType.RELATION) {
          relMember.setType("relation");
        } else {
          assert false; // TODO; Illegal file?
        }

        tmp.addMember(relMember);
      }

      osmdb.addRelation(tmp);
    }
  }

  @Override
  protected void parseDense(Osmformat.DenseNodes nodes) {
    long lastId = 0;
    long lastLat = 0;
    long lastLon = 0;
    int j = 0; // Index into the keysvals array.

    if (parsePhase != OSMParserPhase.NODES) {
      return;
    }

    for (int i = 0; i < nodes.getIdCount(); i++) {
      OSMNode tmp = new OSMNode();

      long lat = nodes.getLat(i) + lastLat;
      lastLat = lat;
      long lon = nodes.getLon(i) + lastLon;
      lastLon = lon;
      long id = nodes.getId(i) + lastId;
      lastId = id;
      double latf = parseLat(lat);
      double lonf = parseLon(lon);

      tmp.setId(id);
      tmp.lat = latf;
      tmp.lon = lonf;

      // If empty, assume that nothing here has keys or vals.
      if (nodes.getKeysValsCount() > 0) {
        while (nodes.getKeysVals(j) != 0) {
          int keyid = nodes.getKeysVals(j++);
          int valid = nodes.getKeysVals(j++);

          OSMTag tag = new OSMTag();
          String key = internalize(getStringById(keyid));
          String value = internalize(getStringById(valid));
          tag.setK(key);
          tag.setV(value);
          tmp.addTag(tag);
        }
        j++; // Skip over the '0' delimiter.
      }

      osmdb.addNode(tmp);
    }
  }

  @Override
  protected void parseNodes(List<Osmformat.Node> nodes) {
    if (parsePhase != OSMParserPhase.NODES) {
      return;
    }

    for (Osmformat.Node i : nodes) {
      OSMNode tmp = new OSMNode();
      tmp.setId(i.getId());
      tmp.lat = parseLat(i.getLat());
      tmp.lon = parseLon(i.getLon());

      for (int j = 0; j < i.getKeysCount(); j++) {
        String key = internalize(getStringById(i.getKeys(j)));
        // if handler.retain_tag(key) // TODO: filter tags
        String value = internalize(getStringById(i.getVals(j)));
        OSMTag tag = new OSMTag();
        tag.setK(key);
        tag.setV(value);
        tmp.addTag(tag);
      }

      osmdb.addNode(tmp);
    }
  }

  @Override
  protected void parseWays(List<Osmformat.Way> ways) {
    if (parsePhase != OSMParserPhase.WAYS) {
      return;
    }

    for (Osmformat.Way i : ways) {
      OSMWay tmp = new OSMWay();
      tmp.setId(i.getId());

      for (int j = 0; j < i.getKeysCount(); j++) {
        OSMTag tag = new OSMTag();
        String key = internalize(getStringById(i.getKeys(j)));
        String value = internalize(getStringById(i.getVals(j)));
        tag.setK(key);
        tag.setV(value);
        tmp.addTag(tag);
      }

      long lastId = 0;
      for (long j : i.getRefsList()) {
        OSMNodeRef nodeRef = new OSMNodeRef();
        nodeRef.setRef(j + lastId);
        tmp.addNodeRef(nodeRef);

        lastId = j + lastId;
      }

      osmdb.addWay(tmp);
    }
  }

  @Override
  public void parse(Osmformat.HeaderBlock block) {
    for (String s : block.getRequiredFeaturesList()) {
      if (s.equals("OsmSchema-V0.6")) {
        continue; // We can parse this.
      }
      if (s.equals("DenseNodes")) {
        continue; // We can parse this.
      }
      throw new IllegalStateException("File requires unknown feature: " + s);
    }
  }
}
