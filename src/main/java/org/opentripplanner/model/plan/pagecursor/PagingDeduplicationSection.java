package org.opentripplanner.model.plan.pagecursor;

/**
 * The PagingDeduplicationSection enum is used to signal which part of an itinerary list may contain
 * duplicates. When paging it is the opposite of the CropSection defined in ListSection. That is, if
 * the list of itineraries was cropped at the bottom, then any duplicates will appear at the top of
 * the list and vice versa.
 */
public enum PagingDeduplicationSection {
  HEAD,
  TAIL,
}
