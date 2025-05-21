package org.opentripplanner.routing.algorithm.filterchain.filters.system.mcmax;

import org.opentripplanner.model.plan.Itinerary;

/**
 * An item is a decorated itinerary. The extra information added is the index in the input list
 * (sort order) and a groupCount. The sort order is used to break ties, while the group-count is
 * used to select the itinerary which exist in the highest number of groups. The group dynamically
 * updates the group-count; The count is incremented when an item is added to a group, and
 * decremented when the group is removed from the State.
 */
class Item {

  private final Itinerary item;
  private final int index;
  private int groupCount = 0;

  Item(Itinerary item, int index) {
    this.item = item;
    this.index = index;
  }

  /**
   * An item is better than another if the groupCount is higher, and in case of a tie, if the sort
   * index is lower.
   */
  public boolean betterThan(Item o) {
    return groupCount != o.groupCount ? groupCount > o.groupCount : index < o.index;
  }

  Itinerary item() {
    return item;
  }

  void incGroupCount() {
    ++this.groupCount;
  }

  void decGroupCount() {
    --this.groupCount;
  }

  @Override
  public String toString() {
    return "Item #%d {count:%d, %s}".formatted(index, groupCount, item.toStr());
  }
}
