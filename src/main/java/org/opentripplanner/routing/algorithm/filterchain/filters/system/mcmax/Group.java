package org.opentripplanner.routing.algorithm.filterchain.filters.system.mcmax;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The purpose of a group is to maintain a list of items, all optimal for a single
 * criteria/comparator. After the group is created, then the criteria is no longer needed, so we do
 * not keep a reference to the original criteria.
 */
class Group implements Iterable<Item> {

  private final List<Item> items = new ArrayList<>();

  public Group(Item firstItem) {
    add(firstItem);
  }

  Item first() {
    return items.getFirst();
  }

  boolean isEmpty() {
    return items.isEmpty();
  }

  boolean isSingleItemGroup() {
    return items.size() == 1;
  }

  void add(Item item) {
    item.incGroupCount();
    items.add(item);
  }

  void removeAllItems() {
    items.forEach(Item::decGroupCount);
    items.clear();
  }

  void addNewDominantItem(Item item) {
    removeAllItems();
    add(item);
  }

  boolean contains(Item item) {
    return this.items.contains(item);
  }

  @Override
  public Iterator<Item> iterator() {
    return items.iterator();
  }
}
