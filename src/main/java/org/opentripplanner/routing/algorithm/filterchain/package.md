# ItineraryListFilterChain

ItineraryListFilterChain is a mechanism for post-processing itinerary results. It contains a list of
Each of which contains different business logic for a different operation. They not only remove
itineraries, but can be also used for sorting, decorating or even adding new itineraries. The filter
chain is also responsible for creating routing errors, if no itineraries remain after filtering.
Itineraries are flagged for deletion, but not actually deleted when debugging is turned on. When
debugging is off, the chain actually removes those itineraries that were flagged for deletion.

![Architecture diagram](images/ItineraryListFilterChain.svg)

There are four types of filters, which can be included in the filter chain. The same type of filter
can appear multiple times.

## DeletionFlaggingFilter

DeletionFlaggingFilter is responsible for flagging itineraries for deletion. It does not remove any
itineraries directly, but uses `Itinerary#flagForDeletion(SystemNotice)` for this. A
DeletionFlaggingFilter is instantiated with a ItineraryDeletionFlagger, which contains the business
logic for selecting the itineraries for flagging. You can use `skipAlreadyFlaggedItineraries()` for
selecting if the filter should skip already flagged itineraries to the flagger. This is useful to
disable, in case already removed itineraries are useful in comparing whether other itineraries
should be flagged for removal.

## SortingFilter

SortingFilter is responsible for sorting the itineraries. It does this by having a Comparator, which
is used for sorting.

## GroupByFilter

GroupByFilter is used to group itineraries together using a `GroupId`, and running a set of filters
on that subset of itineraries. This is used eg. to remove almost similar search results and to sort
them, so that only the best are shown to the user.

## DecoratingFilter

DecorationgFilter can be used to decorate the itineraries. This can be used eg to add information
about ticketing and fares for each itinerary, and refining the routing cost of the itinerary, which
might affect the sorting order of the itineraries, depending on the order of the filters.