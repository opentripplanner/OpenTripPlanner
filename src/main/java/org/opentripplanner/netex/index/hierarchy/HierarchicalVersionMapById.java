package org.opentripplanner.netex.index.hierarchy;

import static org.opentripplanner.netex.support.NetexVersionHelper.latestVersionIn;
import static org.opentripplanner.netex.support.NetexVersionHelper.latestVersionedElementIn;
import static org.opentripplanner.netex.support.NetexVersionHelper.versionOf;

import com.google.common.collect.Multimap;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalVersionMapById;
import org.rutebanken.netex.model.EntityInVersionStructure;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

/**
 * A hierarchical multimap indexing a collections of {@link EntityInVersionStructure} values by
 * their {@code id}. Use the one argument {@link #add(EntityInVersionStructure)} method to add
 * elements to the map.
 *
 * @param <V> the value type
 */
public class HierarchicalVersionMapById<V extends EntityInVersionStructure>
  extends HierarchicalMultimap<String, V>
  implements ReadOnlyHierarchicalVersionMapById<V> {

  /** Create a root for the hierarchy */
  public HierarchicalVersionMapById() {}

  /** Create a child of the given {@code parent}. */
  public HierarchicalVersionMapById(HierarchicalVersionMapById<V> parent) {
    super(parent);
  }

  /** Return a reference to the parent. */
  @Override
  public HierarchicalVersionMapById<V> parent() {
    return (HierarchicalVersionMapById<V>) super.parent();
  }

  /**
   * Use the {@link #add(EntityInVersionStructure)} method!
   *
   * @throws IllegalArgumentException This method throws an exception to prevent adding elements
   *                                  with a key different than the element id.
   */
  @Override
  public void add(String key, V value) {
    throw new IllegalArgumentException("Use the add method with just one argument instead.");
  }

  @Override
  // We need to override this method because the super method uses the the #add(Strinng, V)
  // method - which throws an exception.
  public void addAll(Multimap<String, V> other) {
    throw new IllegalArgumentException("Use the add method with just one argument instead.");
  }

  public Collection<String> localKeys() {
    return super.localKeys();
  }

  /**
   * Add an entity and use its Id as key to index it.
   */
  public void add(V entity) {
    super.add(entity.getId(), entity);
  }

  /** Add all given entities to local map */
  public void addAll(Collection<V> entities) {
    for (V it : entities) {
      add(it);
    }
  }

  @Override
  public V lookupLastVersionById(String id) {
    return latestVersionedElementIn(lookup(id));
  }

  @Override
  public V lookup(VersionOfObjectRefStructure ref, LocalDateTime timestamp) {
    String id = ref.getRef();
    String version = ref.getVersion();

    Collection<V> list = lookup(id);

    if (list.isEmpty()) {
      return null;
    }

    if (version != null) {
      for (V value : list) {
        if (version.equals(value.getVersion())) {
          return value;
        }
      }
    }
    // Fallback to the latest version of the element.
    return firstValidBestVersion(list, timestamp);
  }

  @Override
  public Collection<V> localListCurrentVersionEntities(final LocalDateTime timestamp) {
    return localValues()
      .stream()
      .map(c -> firstValidBestVersion(c, timestamp))
      .filter(Objects::nonNull)
      .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public boolean isNewerOrSameVersionComparedWithExistingValues(V value) {
    return versionOf(value) >= latestVersionIn(lookup(value.getId()));
  }

  private V firstValidBestVersion(Collection<V> entities, LocalDateTime timestamp) {
    return entities
      .stream()
      .map(it -> new ValidOnDate<>(it, timestamp))
      .filter(ValidOnDate::isValid)
      .reduce((a, b) -> a.bestVersion(b) ? a : b)
      .map(ValidOnDate::entity)
      .orElse(null);
  }
}
