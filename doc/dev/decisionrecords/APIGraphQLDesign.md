# GraphQL Best Practices - API Design

Follow best practices for designing GraphQL APIs. Our APIs need to be backwards compatible as they
are used by hundreds of clients (web-pages/apps/services). A good reference used by several
of the OTP developers is the Production Ready GraphQL book by Marc-André Giroux.


## Pagination

We use the [pagination](https://graphql.org/learn/pagination/) (a.k.a. Relay) specification for paging over entities like stations, 
stops, trips and routes. Very often OTP has a _finite_ list of entities in memory. The route request has an OTP custom pagination
feature - it is not finite and very complex.


## Refetching

We often use `type(id)` format queries for fetching or refetching entities or value objects of type
by id. Additionally, the GTFS GraphQL API has a node interface and query for refetching objects 
which follow the [GraphQL Global Object Identification Specification](https://relay.dev/graphql/objectidentification.htm). 
We should not use the node interface or query for non-entities (such as Itineraries and Legs) which
do not always have an ID and/or which IDs are not trivial to reconstruct.


## Consistency

Unfortunately, part of the GraphQL API is old and does not follow best practices. So, when adding
new features, consider what is best; To follow the existing style or follow the best practice. 
    

### Context and Problem Statement

Our APIs need to be backwards compatible as they are used by hundreds of clients
(web-pages/apps/services). Correcting mistakes may not be possible or may take a long time.

We allow breaking API changes in these cases:

- During incremental development of a new feature (multiple PRs). The new part of the API should be
  documented as _"WorkInProgress - Why/When is it ready"_. A new API should not stay "open" for a
  long time.
- Deprecated features can be removed after 2 years (4 minor releases) unless a user wants to keep 
  it. We will revert a removal if the request to keep it comes after the feature is removed. 
  Example:

> Use feature xyz instead. 
> Note! This feature is subject for removal in September 2027! If you want this to stay, please
>       notify the OTP community.

## Conventions

### Small `input` value-objects should have required fields, no default field values

An input representing a single concept or thing should carry all relevant fields even if
some of the values are common or have a natural default. Examples of such input types are 
`InputCoordinate(lat, long)` and `InputLinearFunction(constant, coefficient)`. Small input 
value-object types could be scalars, but if they have multiple fields using an input type 
is a better match. This simplifies validation and default value injection.
