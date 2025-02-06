package org.opentripplanner.osm;

import crosby.binary.BinaryParser;
import crosby.binary.Osmformat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.osm.model.OsmMemberType;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmRelation;
import org.opentripplanner.osm.model.OsmRelationMember;
import org.opentripplanner.osm.model.OsmTag;
import org.opentripplanner.osm.model.OsmWay;

/**
 * Parser for the OpenStreetMap PBF Format.
 *
 * @since 0.4
 */
class OsmParser extends BinaryParser {

  private final OsmDatabase osmdb;
  private final Map<String, String> stringTable = new HashMap<>();
  private final DefaultOsmProvider provider;
  private OsmParserPhase parsePhase;

  public OsmParser(OsmDatabase osmdb, DefaultOsmProvider provider) {
    this.osmdb = Objects.requireNonNull(osmdb);
    this.provider = Objects.requireNonNull(provider);
  }

  // The strings are already being pulled from a string table in the PBF file,
  // but there appears to be a separate string table per 8k-entry PBF file block.
  // String.intern grinds to a halt on large PBF files (as it did on GTFS import), so
  // we implement our own.
  public String internalize(String s) {
    String fromTable = stringTable.get(s);
    if (fromTable == null) {
      stringTable.put(s, s);
      return s;
    }
    return fromTable;
  }

  @Override
  public void complete() {
    // Jump in circles
  }

  /**
   * Set the phase to be parsed
   */
  public void setPhase(OsmParserPhase phase) {
    this.parsePhase = phase;
  }

  @Override
  protected void parseRelations(List<Osmformat.Relation> rels) {
    if (parsePhase != OsmParserPhase.Relations) {
      return;
    }

    for (Osmformat.Relation i : rels) {
      OsmRelation tmp = new OsmRelation();
      tmp.setId(i.getId());
      tmp.setOsmProvider(provider);

      for (int j = 0; j < i.getKeysCount(); j++) {
        OsmTag tag = new OsmTag();
        String key = internalize(getStringById(i.getKeys(j)));
        String value = internalize(getStringById(i.getVals(j)));
        tag.setK(key);
        tag.setV(value);
        tmp.addTag(tag);
      }

      long lastMid = 0;
      for (int j = 0; j < i.getMemidsCount(); j++) {
        OsmRelationMember relMember = new OsmRelationMember();
        long mid = lastMid + i.getMemids(j);

        relMember.setRef(mid);
        lastMid = mid;

        relMember.setRole(internalize(getStringById(i.getRolesSid(j))));

        if (i.getTypes(j) == Osmformat.Relation.MemberType.NODE) {
          relMember.setType(OsmMemberType.NODE);
        } else if (i.getTypes(j) == Osmformat.Relation.MemberType.WAY) {
          relMember.setType(OsmMemberType.WAY);
        } else if (i.getTypes(j) == Osmformat.Relation.MemberType.RELATION) {
          relMember.setType(OsmMemberType.RELATION);
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
    long lastId = 0, lastLat = 0, lastLon = 0;
    int j = 0; // Index into the keysvals array.

    if (parsePhase != OsmParserPhase.Nodes) {
      return;
    }

    for (int i = 0; i < nodes.getIdCount(); i++) {
      OsmNode tmp = new OsmNode();

      long lat = nodes.getLat(i) + lastLat;
      lastLat = lat;
      long lon = nodes.getLon(i) + lastLon;
      lastLon = lon;
      long id = nodes.getId(i) + lastId;
      lastId = id;
      double latf = parseLat(lat), lonf = parseLon(lon);

      tmp.setId(id);
      tmp.setOsmProvider(provider);
      tmp.lat = latf;
      tmp.lon = lonf;

      // If empty, assume that nothing here has keys or vals.
      if (nodes.getKeysValsCount() > 0) {
        while (nodes.getKeysVals(j) != 0) {
          int keyid = nodes.getKeysVals(j++);
          int valid = nodes.getKeysVals(j++);

          OsmTag tag = new OsmTag();
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
    if (parsePhase != OsmParserPhase.Nodes) {
      return;
    }

    for (Osmformat.Node i : nodes) {
      OsmNode tmp = new OsmNode();
      tmp.setId(i.getId());
      tmp.setOsmProvider(provider);
      tmp.lat = parseLat(i.getLat());
      tmp.lon = parseLon(i.getLon());

      for (int j = 0; j < i.getKeysCount(); j++) {
        String key = internalize(getStringById(i.getKeys(j)));
        // if handler.retain_tag(key) // TODO: filter tags
        String value = internalize(getStringById(i.getVals(j)));
        OsmTag tag = new OsmTag();
        tag.setK(key);
        tag.setV(value);
        tmp.addTag(tag);
      }

      osmdb.addNode(tmp);
    }
  }

  @Override
  protected void parseWays(List<Osmformat.Way> ways) {
    if (parsePhase != OsmParserPhase.Ways) {
      return;
    }

    for (Osmformat.Way i : ways) {
      OsmWay tmp = new OsmWay();
      tmp.setId(i.getId());
      tmp.setOsmProvider(provider);

      for (int j = 0; j < i.getKeysCount(); j++) {
        OsmTag tag = new OsmTag();
        String key = internalize(getStringById(i.getKeys(j)));
        String value = internalize(getStringById(i.getVals(j)));
        tag.setK(key);
        tag.setV(value);
        tmp.addTag(tag);
      }

      long lastId = 0;
      for (long j : i.getRefsList()) {
        tmp.addNodeRef(j + lastId);
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
