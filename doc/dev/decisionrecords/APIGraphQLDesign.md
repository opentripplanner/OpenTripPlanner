# GraphQL Best Practices - API Design

Follow best practices for designing GraphQL APIs. Our APIs need to be backwards compatible as they
are used by hundreds of clients (web-pages/apps/services). A good reference used by several
of the OTP developers is the Production Ready GraphQL book by Marc-André Giroux.


## Pagination

We use the [pagination](https://graphql.org/learn/pagination/) (a.k. Relay) specification for paging over entities like stations, 
stops, trips and routes. Very often OTP has a _finite_ list of entities in memory.


## Refetching

We often use `type(id)` format queries for fetching or refetching entities or value objects of type by id. Additionally,
the GTFS GraphQL API has a node interface and query for refetching objects which follow the
[GraphQL Global Object Identification Specification](https://relay.dev/graphql/objectidentification.htm). We should not use the
node interface or query for non-entities (such as Itineraries and Legs) which do not always have an ID and/or which IDs are not
trivial to reconstruct.


## Consistency

Unfortunately, part of the GraphQL API is old and does not follow best practices. So, when adding
new features, consider what is best; To follow the existing style or follow the best practice. 
    

### Context and Problem Statement

Our APIs need to be backwards compatible as they are used by hundreds of clients (web-pages/apps/services)
Correcting mistakes may not be possible or may take a long time. 

