package org.opentripplanner.netex.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Arrange zip file entries into a hierarchy:
 * <pre>
 *     1. Shared files               -- a set of shared files
 *     2. Group files                -- a set of files grouped by a naming convention
 *         2.1 Shared group files        -- Shared within the group
 *         2.2 (Individual) Group files  -- Not shared
 * </pre>
 * <p>
 * The files is loaded in the hierarchical order. First the <em>Shared files</em>, then for each
 * group: shared group files are loaded before individual group files.
 * <p>
 * All NeTEx entities are cached in an index made available for reference linking. To save memory
 * shared group files entities are discarded after the group is loaded (and linking is complete).
 * Entities in individual group files are discarded after the file entry is loaded.
 */
public class NetexDataSourceHierarchy {

  private static final Logger LOG = LoggerFactory.getLogger(NetexDataSourceHierarchy.class);

  private final CompositeDataSource source;
  private final List<DataSource> sharedEntries = new ArrayList<>();
  private final Map<String, GroupEntries> groupEntries = new TreeMap<>();

  public NetexDataSourceHierarchy(CompositeDataSource source) {
    this.source = source;
  }

  public NetexDataSourceHierarchy prepare(
    Pattern ignoreFilePattern,
    Pattern sharedFilePattern,
    Pattern sharedGroupFilePattern,
    Pattern groupFilePattern
  ) {
    new DistributeEntries(
      ignoreFilePattern,
      sharedFilePattern,
      sharedGroupFilePattern,
      groupFilePattern
    ).execute();
    return this;
  }

  public String description() {
    return source.path();
  }

  public Iterable<DataSource> sharedEntries() {
    return sharedEntries;
  }

  public Iterable<GroupEntries> groups() {
    return groupEntries.values();
  }

  /**
   * Process the data source and distribute entries to {@code sharedEntries} and {@code
   * groupEntries}.
   */
  private class DistributeEntries {

    private final Pattern ignoreFilePattern;
    private final Pattern sharedFilePattern;
    private final Pattern sharedGroupFilePattern;
    private final Pattern groupFilePattern;

    private String currentGroup = null;

    private DistributeEntries(
      Pattern ignoreFilePattern,
      Pattern sharedFilePattern,
      Pattern sharedGroupFilePattern,
      Pattern groupFilePattern
    ) {
      this.ignoreFilePattern = ignoreFilePattern;
      this.sharedFilePattern = sharedFilePattern;
      this.sharedGroupFilePattern = sharedGroupFilePattern;
      this.groupFilePattern = groupFilePattern;
    }

    private void execute() {
      for (DataSource entry : source.content()) {
        String name = entry.name();

        if (ignoredFile(name)) {
          LOG.debug("Netex file ignored: {}.", name);
        } else if (isSharedFile(name)) {
          sharedEntries.add(entry);
        } else if (isGroupEntry(name, sharedGroupFilePattern)) {
          groupEntries.get(currentGroup).addSharedEntry(entry);
        } else if (isGroupEntry(name, groupFilePattern)) {
          groupEntries.get(currentGroup).addIndependentEntries(entry);
        } else {
          LOG.warn(
            "Netex file ignored: {}. The file do not " + "match any file patterns in the config.",
            name
          );
        }
      }
    }

    private boolean ignoredFile(String name) {
      return ignoreFilePattern.matcher(name).matches();
    }

    private boolean isSharedFile(String name) {
      return sharedFilePattern.matcher(name).matches();
    }

    private boolean isGroupEntry(String name, Pattern filePattern) {
      Matcher m = filePattern.matcher(name);
      if (!m.matches()) {
        return false;
      }
      try {
        currentGroup = m.group(1);
      } catch (IndexOutOfBoundsException e) {
        throw new IllegalStateException(
          "Netex file patten '" +
          filePattern +
          "' is missing a group pattern like: '(\\w+)' in '(\\w+)-.*\\.xml' "
        );
      }
      groupEntries.computeIfAbsent(currentGroup, GroupEntries::new);
      return true;
    }
  }
}
