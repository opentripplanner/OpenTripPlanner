package org.opentripplanner.raptor.util.paretoset;

/**
 * {@link ParetoSet} with the possibility to set an index marker, which can be used to list all
 * elements added after the marker is set.
 *
 * @param <T> the element type
 */
public class ParetoSetWithMarker<T> extends ParetoSet<T> {

  private int marker = 0;

  public ParetoSetWithMarker(ParetoComparator<T> comparator) {
    super(comparator);
  }

  public ParetoSetWithMarker(
    ParetoComparator<T> comparator,
    ParetoSetEventListener<? super T> eventListener
  ) {
    super(comparator, eventListener);
  }

  @Override
  public void clear() {
    super.clear();
    marker = 0;
  }

  @Override
  protected void notifyElementMoved(int fromIndex, int toIndex) {
    if (fromIndex == marker) {
      marker = toIndex;
    }
  }

  /**
   * List all elements added after the marker.
   */
  public Iterable<T> elementsAfterMarker() {
    return tail(marker);
  }

  /**
   * Move the marker after the last element in the set.
   */
  public void markAtEndOfSet() {
    marker = size();
  }
}
