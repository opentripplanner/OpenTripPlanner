
## Qualifiers

This package contains qualifiers for sandbox itinerary-filter-chain extensions. If a sandbox filter 
does not need any configuration input, then using a qualifier annotation and one of the spi 
filter-chain roles(`ItineraryDecorator`,  `ItineraryListFilter`, `RemoveItineraryFlagger`) is a 
good way to avoid dependencies from the itinerary-filter-chain to the sandbox module. 

