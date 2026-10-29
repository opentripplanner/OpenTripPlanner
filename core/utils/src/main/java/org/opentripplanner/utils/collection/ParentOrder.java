package org.opentripplanner.utils.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Order items that refer to a parent item by id, so a parent always comes before its children. The
 * references form a forest: an item has at most one parent, and a cycle is an error.
 * <p>
 * The order is stable: items keep their relative order, except that an item that has a parent
 * is moved to just after the parent if it comes first. This is useful when an item is created
 * from its parent, but the declaration order is free, like in a configuration file.
 */
public final class ParentOrder {

  private ParentOrder() {}

  /**
   * Return the items with every parent before its children.
   *
   * @param id     the id of an item, must be unique
   * @param parent the id of the parent of an item, {@code null} for a root
   * @throws IllegalArgumentException if two items have the same id, an item is its own parent, a
   *                                  parent does not exist, or the parents form a cycle. The
   *                                  message names the ids.
   */
  public static <T> List<T> parentsFirst(
    Collection<T> items,
    Function<T, String> id,
    Function<T, String> parent
  ) {
    var byId = new LinkedHashMap<String, T>();
    for (var item : items) {
      if (byId.put(id.apply(item), item) != null) {
        throw new IllegalArgumentException("Duplicate id: '" + id.apply(item) + "'");
      }
    }
    var result = new ArrayList<T>(items.size());
    var done = new HashSet<String>();
    for (var item : items) {
      visit(item, byId, id, parent, done, new ArrayList<>(), result);
    }
    return List.copyOf(result);
  }

  private static <T> void visit(
    T item,
    Map<String, T> byId,
    Function<T, String> id,
    Function<T, String> parent,
    Set<String> done,
    List<String> path,
    List<T> result
  ) {
    var itemId = id.apply(item);
    if (done.contains(itemId)) {
      return;
    }
    if (path.contains(itemId)) {
      var cycle = new ArrayList<>(path.subList(path.indexOf(itemId), path.size()));
      cycle.add(itemId);
      throw new IllegalArgumentException(
        "The parents form a cycle: '" + String.join("' -> '", cycle) + "'"
      );
    }
    var parentId = parent.apply(item);
    if (parentId != null) {
      var parentItem = byId.get(parentId);
      if (parentItem == null) {
        throw new IllegalArgumentException(
          "The parent '" + parentId + "' of '" + itemId + "' does not exist"
        );
      }
      path.add(itemId);
      visit(parentItem, byId, id, parent, done, path, result);
      path.removeLast();
    }
    done.add(itemId);
    result.add(item);
  }
}
