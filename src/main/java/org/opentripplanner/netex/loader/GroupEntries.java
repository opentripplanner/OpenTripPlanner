package org.opentripplanner.netex.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.datastore.api.DataSource;

/**
 * A named group of NeTEx file entries. The entries are grouped together with a set of shared group
 * entries and a set of _individual_ entries.
 */
public class GroupEntries {

  private final String name;
  private final List<DataSource> sharedEntries = new ArrayList<>();
  private final List<DataSource> entries = new ArrayList<>();

  GroupEntries(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }

  public Collection<DataSource> sharedEntries() {
    return sharedEntries;
  }

  public Collection<DataSource> independentEntries() {
    return entries;
  }

  void addSharedEntry(DataSource entry) {
    sharedEntries.add(entry);
  }

  void addIndependentEntries(DataSource entry) {
    entries.add(entry);
  }
}
