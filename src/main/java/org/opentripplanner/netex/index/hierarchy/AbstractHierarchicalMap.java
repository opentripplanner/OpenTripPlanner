package org.opentripplanner.netex.index.hierarchy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.index.api.HMapValidationRule;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;

/**
 * Base class for a hierarchical map. This class proved a way to create a hierarchy of maps with a
 * parent - child relationship. Elements must be added to the right level (map instance), but when
 * retrieving values({@link #lookup(Object)}) the lookup call check the current instance, then ask
 * the parent. This continue until the root of the hierarchy is reached. If a {@code key} exist in
 * more than two places in the hierarchy, the first value found wins.
 * <p>
 * There is no reference from the parent to the child, enableing garbage collection of children,
 * when not referenced by the outer context any more.
 *
 * @param <K> The key type
 * @param <V> Thr value type
 */
public abstract class AbstractHierarchicalMap<K, V> implements ReadOnlyHierarchicalMap<K, V> {

  private final AbstractHierarchicalMap<K, V> parent;

  AbstractHierarchicalMap(AbstractHierarchicalMap<K, V> parent) {
    this.parent = parent;
  }

  /**
   * Lookup element, if not found delegate up to the parent. NB! elements of this class and its
   * parents are NOT merged, the closest win.
   *
   * @return an empty collection if no element are found.
   */
  @Override
  public V lookup(K key) {
    return (localContainsKey(key) || isRoot()) ? localGet(key) : parent.lookup(key);
  }

  /**
   * The key exist in this Collection or one of the parents (parent, parent´s parent and so on)
   */
  @Override
  public boolean containsKey(K key) {
    return localContainsKey(key) || parentContainsKey(key);
  }

  /**
   * The size of the collection including any parent nodes. Returns the number of key-value pairs
   * for a Map.
   */
  public int size() {
    return localSize() + (isRoot() ? 0 : parent.localSize());
  }

  /**
   * Validate a stateless rule.
   */
  public void validate(HMapValidationRule<K, V> rule, Consumer<DataImportIssue> warnMsgConsumer) {
    List<K> discardKeys = new ArrayList<>();
    for (K key : localKeys()) {
      V value = localGet(key);

      HMapValidationRule.Status status = rule.validate(value);

      if (status == HMapValidationRule.Status.DISCARD) {
        discardKeys.add(key);
      }
      if (status != HMapValidationRule.Status.OK) {
        warnMsgConsumer.accept(rule.logMessage(key, value));
      }
    }
    discardKeys.forEach(this::localRemove);
  }

  /**
   * Validate a stateful rule.
   */
  public void validate(
    Supplier<HMapValidationRule<K, V>> ruleSupplier,
    Consumer<DataImportIssue> warnMsgConsumer
  ) {
    List<K> discardKeys = new ArrayList<>();
    for (K key : localKeys()) {
      V value = localGet(key);
      HMapValidationRule<K, V> rule = ruleSupplier.get();
      HMapValidationRule.Status status = rule.validate(value);

      if (status == HMapValidationRule.Status.DISCARD) {
        discardKeys.add(key);
      }
      if (status != HMapValidationRule.Status.OK) {
        warnMsgConsumer.accept(rule.logMessage(key, value));
      }
    }
    discardKeys.forEach(this::localRemove);
  }

  /** Return a reference to the parent. */
  public AbstractHierarchicalMap<K, V> parent() {
    return parent;
  }

  @Override
  public String toString() {
    // It helps in debugging to se the size before expanding the element
    return "size = " + size();
  }

  /** Return the size of the collection. Returns the number of key-value pairs for a Map. */
  protected abstract int localSize();

  /** Get value from 'local' map, parent is not queried. */
  abstract V localGet(K key);

  /** Check if key exist in 'local' map, parent is not queried. */
  abstract boolean localContainsKey(K key);

  /* private methods */

  /** Remove local value from collection. */
  abstract void localRemove(K key);

  /** Return true if this instance have a parent. */
  private boolean isRoot() {
    return parent == null;
  }

  /**
   * Return true if the {@code key} exist in one of the parents (parent, parent´s parent and so
   * on).
   */
  private boolean parentContainsKey(K key) {
    return parent != null && parent.containsKey(key);
  }
}
