package org.opentripplanner.modularization.harness;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The list of accepted differences between dev-2.x and the modularized tree.
 *
 * <p>An entry matches a difference when the scenario matches and the path matches. Both are globs
 * where {@code *} matches anything. Every entry needs a reason and an owner. The list may only
 * shrink: an entry that matches no difference is reported as unused and fails the comparison.
 */
public final class ApprovedDiscrepancies {

  /** One approved entry. */
  public record Entry(Glob scenario, Glob path, String reason, String owner) {
    boolean matches(Difference d) {
      return scenario.matches(d.scenario()) && path.matches(d.path());
    }

    @Override
    public String toString() {
      return "scenario=%s path=%s (owner: %s, reason: %s)".formatted(
        scenario,
        path,
        owner,
        reason
      );
    }
  }

  /** The result of checking differences against the approved list. */
  public record Result(List<Difference> unapproved, List<Entry> unused) {
    public boolean isCompliant() {
      return unapproved.isEmpty() && unused.isEmpty();
    }
  }

  private final List<Entry> entries;

  public ApprovedDiscrepancies(List<Entry> entries) {
    this.entries = List.copyOf(entries);
  }

  public static ApprovedDiscrepancies empty() {
    return new ApprovedDiscrepancies(List.of());
  }

  /** Read the approved file. Format: {@code {"discrepancies":[{"scenario":..,"path":..,"reason":..,"owner":..}]}} */
  public static ApprovedDiscrepancies read(Path file) {
    if (!Files.exists(file)) {
      return empty();
    }
    try {
      JsonNode root = Json.MAPPER.readTree(file.toFile());
      var entries = new ArrayList<Entry>();
      for (JsonNode n : root.path("discrepancies")) {
        String reason = text(n, "reason");
        String owner = text(n, "owner");
        if (reason.isBlank() || owner.isBlank()) {
          throw new IllegalArgumentException(
            "An approved discrepancy needs a reason and an owner: " + n
          );
        }
        entries.add(
          new Entry(new Glob(text(n, "scenario")), new Glob(text(n, "path")), reason, owner)
        );
      }
      return new ApprovedDiscrepancies(entries);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Result check(List<Difference> differences) {
    var used = new HashSet<Entry>();
    var unapproved = new ArrayList<Difference>();
    for (Difference d : differences) {
      boolean approved = false;
      for (Entry e : entries) {
        if (e.matches(d)) {
          used.add(e);
          approved = true;
        }
      }
      if (!approved) {
        unapproved.add(d);
      }
    }
    Set<Entry> usedSet = used;
    var unused = entries.stream().filter(e -> !usedSet.contains(e)).toList();
    return new Result(unapproved, unused);
  }

  private static String text(JsonNode n, String field) {
    return n.path(field).asText("");
  }
}
