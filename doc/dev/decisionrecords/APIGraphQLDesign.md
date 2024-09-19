# GraphQL Best Practices - API Design

Follow best practices for designing GraphQL APIs. Our APIs are used by hundreds of clients
(web-pages/apps/services) and need to be backwards compatible. A good reference used by several
of the OTP developers are the Production Ready GraphQL book by Marc-André Giroux.


## Pagination

We use the [pagination](https://graphql.org/learn/pagination/) (a.k. Relay) specification for paging over entities like station, 
stops, trips and routes. Very often OTP has a _final_ list of entities in memory. For non-entities
(Itinerary and Legs), witch does not always have an ID and is none trivial to reconstruct, it is 
better to make a custom solution. 


## Consistency

Unfortunately, part of the GraphQL API is old and does not follow best practices. So, when adding
new features, consider what is best; To follow the existing style or follow the best practice. 
    

### Context and Problem Statement

Our APIs are used by hundreds of clients(web-pages/apps/services) and need to backwards compatible.
Correcting mistakes may not be possible or may take a long time. 

