package org.opentripplanner.routing.algorithm.filterchain.filters.system.mcmax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.SingleCriteriaComparator;

/**
 * Keep a list of items, groups and the result in progress. This is just a class for
 * simple bookkeeping for the state of the filter.
 */
class State {

  private final List<Item> items;
  private final List<Group> groups;
  private final List<Item> result = new ArrayList<>();

  /**
   * Initialize the state by wrapping each itinerary in an item (with index) and create groups for
   * each criterion with the best itineraries (can be more than one with, for example, the same
   * cost). There should be at least one itinerary from each group surviving the filtering process.
   * The same itinerary can exist in multiple groups.
   */
  State(List<Itinerary> itineraries, List<SingleCriteriaComparator> comparators) {
    this.items = createListOfItems(itineraries);
    this.groups = createGroups(items, comparators);
  }

  List<Itinerary> getResult() {
    return result.stream().map(Item::item).toList();
  }

  /**
   * Find and add all groups with a single item in them and add them to the result
   */
  void findAllSingleItemGroupsAndAddTheItemToTheResult() {
    var item = findItemInFirstSingleItemGroup(groups);
    while (item != null) {
      addToResult(item);
      item = findItemInFirstSingleItemGroup(groups);
    }
  }

  /**
   * Find the items with the highest group count and the lowest index. Theoretically, there might be
   * a smaller set of itineraries that TOGETHER represent all groups than what we achieve here, but
   * it is far more complicated to compute - so this is probably good enough.
   */
  void findTheBestItemsUntilAllGroupsAreRepresentedInTheResult() {
    while (!groups.isEmpty()) {
      addToResult(findBestItem(groups));
    }
  }

  /**
   * Fill up with itineraries until the minimum number of itineraries is reached
   */
  void fillUpTheResultWithMinimumNumberOfItineraries(int minNumItineraries) {
    int end = Math.min(items.size(), minNumItineraries);
    for (int i = 0; result.size() < end; ++i) {
      var it = items.get(i);
      if (!result.contains(it)) {
        result.add(it);
      }
    }
  }

  private void addToResult(Item item) {
    result.add(item);
    removeGroupsWitchContainsItem(item);
  }

  /**
   * If an itinerary is accepted into the final result, then all groups that contain that itinerary
   * can be removed. In addition, the item groupCount should be decremented if a group is dropped.
   * This makes sure that the groups represented in the final result do not count when selecting the
   * next item.
   */
  private void removeGroupsWitchContainsItem(Item item) {
    for (Group group : groups) {
      if (group.contains(item)) {
        group.removeAllItems();
      }
    }
    groups.removeIf(Group::isEmpty);
  }

  /**
   * The best item is the one which exists in most groups, and in case of a tie, the sort order/
   * itinerary index is used.
   */
  private static Item findBestItem(List<Group> groups) {
    var candidate = groups.getFirst().first();
    for (Group group : groups) {
      for (Item item : group) {
        if (item.betterThan(candidate)) {
          candidate = item;
        }
      }
    }
    return candidate;
  }

  /**
   * Search through all groups and return all items which comes from groups with only one item.
   */
  @Nullable
  private static Item findItemInFirstSingleItemGroup(List<Group> groups) {
    return groups
      .stream()
      .filter(Group::isSingleItemGroup)
      .findFirst()
      .map(Group::first)
      .orElse(null);
  }

  private static ArrayList<Item> createListOfItems(List<Itinerary> itineraries) {
    var items = new ArrayList<Item>();
    for (int i = 0; i < itineraries.size(); i++) {
      items.add(new Item(itineraries.get(i), i));
    }
    return items;
  }

  private static List<Group> createGroups(
    Collection<Item> items,
    List<SingleCriteriaComparator> comparators
  ) {
    List<Group> groups = new ArrayList<>();
    for (SingleCriteriaComparator comparator : comparators) {
      if (comparator.strictOrder()) {
        groups.add(createOrderedGroup(items, comparator));
      } else {
        groups.addAll(createUnorderedGroups(items, comparator));
      }
    }
    return groups;
  }

  /**
   * In a strict ordered group only one optimal value exist for the criteria defined by the given
   * {@code comparator}. All items that have this value should be included in the group created.
   */
  private static Group createOrderedGroup(
    Collection<Item> items,
    SingleCriteriaComparator comparator
  ) {
    Group group = null;
    for (Item item : items) {
      if (group == null) {
        group = new Group(item);
        continue;
      }
      var current = group.first();
      if (comparator.leftDominanceExist(item.item(), current.item())) {
        group.addNewDominantItem(item);
      } else if (!comparator.leftDominanceExist(current.item(), item.item())) {
        group.add(item);
      }
    }
    return group;
  }

  /**
   * For a none strict ordered criteria, multiple optimal values exist. The criterion is defined by
   * the given {@code comparator}. This method will create a group for each optimal value found in
   * the given set of items.
   *
   * @see #createOrderedGroup(Collection, SingleCriteriaComparator)
   */
  private static Collection<? extends Group> createUnorderedGroups(
    Collection<Item> items,
    SingleCriteriaComparator comparator
  ) {
    List<Group> result = new ArrayList<>();

    for (Item item : items) {
      int groupCount = result.size();
      for (Group group : result) {
        var groupItem = group.first().item();
        if (comparator.leftDominanceExist(groupItem, item.item())) {
          if (comparator.leftDominanceExist(item.item(), groupItem)) {
            // Mutual dominance => the item belong in another group
            --groupCount;
          }
        } else {
          if (comparator.leftDominanceExist(item.item(), groupItem)) {
            group.removeAllItems();
          }
          group.add(item);
        }
      }
      if (groupCount == 0) {
        result.add(new Group(item));
      }
    }
    return result;
  }
}
