package org.opentripplanner.framework.collection;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class provides a read-only view for a fixed set of collections. It does not copy the
 * elements, instead it keeps a reference to the underlying collections. There is a very small
 * overhead when iterating over the collection and when calculating the size.
 * <p>
 * Use this when you want to concatenate several big {@link Collection}s for read-only access.
 * <p>
 * This list will reflect any changes in the underlying {@link Collection}s, so keeping a reference
 * to it is safe.
 * <p>
 * Any attempts to modify the view will throw an exception - it is READ-ONLY, even when the
 * underlying collections are modifiable.
 * <p>
 * This class is serializable, but be aware that the underlying collections also must be
 * serializable for it to work. The view is very light-weight, so in general it is better to
 * avoid serialization. In most cases you can create a new instance every time you need a view.
 */
public class CollectionsView<T>
  extends AbstractCollection<T>
  implements Collection<T>, Serializable {

  private final Collection<? extends T>[] collections;

  @SafeVarargs
  public CollectionsView(Collection<? extends T>... collections) {
    this.collections = collections;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      int i = 0;
      Iterator<? extends T> it = collections[0].iterator();

      @Override
      public boolean hasNext() {
        if (i == collections.length) {
          return false;
        }
        if (it.hasNext()) {
          return true;
        }
        ++i;
        while (i != collections.length) {
          it = collections[i].iterator();
          if (it.hasNext()) {
            return true;
          }
          ++i;
        }
        it = null;
        return false;
      }

      @Override
      public T next() {
        return it.next();
      }
    };
  }

  @Override
  public int size() {
    int size = 0;
    for (Collection<? extends T> it : collections) {
      size += it.size();
    }
    return size;
  }
}
