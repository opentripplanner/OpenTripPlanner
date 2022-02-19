# OTP2 GraphQL API


## Support for more than one dialect of the GraphQL Schema 

OTP have two endpoint with the same structure, but translated in the vocabulary of GTFS and
Transmodel/NeTEx. We provide these two end-points so organizations using one of these standards have
a "more familiar" API to work with. The two endpoints are equal in functionality and structure, and 
all data, independent of the format of the source, is available in both. We achieve this by using 
one schema kept in the language of the OTP internal model, and translating this into the 
NeTEx and GTFS dialect.

See the [GraphQL Schema Dialect Design](ApiTranslationDesing.md) to get an overview of the 
implementation.


## Schema design


### Goals

When designing the new API we had the following goals:
 - Follow defato standards and best practices for GraphQL schema design. If we don´t follow 
   the principles, then the exception should be documented and we should explain why. We recommend 
   reading the book: *Production Ready GraphQL*, by *Marc-André Giroux*.
 - Prefer human-readable values over compact data or easy integration. For example use 
   "2022-02-14T12:00:59Z" and not an `int` with epoc seconds for representing a time instant. This 
   make it much easier to debug, and even discovering errors.
 - We would like to support both GTFS and Transmodel terminology. The solution should:
   - Try to minimize maintenance
   - Avoid the two versions to evolve separately
   - Both APIs should support the unified OTP model, with all features independent of what the 
     imported date comes from GTFS or NeTEx. 
   - Have the same structure, if possible. This has the downside that we need to introduce *new* 
     levels witch does not exist in GTFS, and also take away levels present in NeTEx.
 - We should have automatic tests at the API level, testing the application wiring and that the 
   mapping works, smoke tests. Parsing and serialization logic should have unit tests. Do NOT add
   test at the API level to test OTP core features like the router.
 - Naming
   - The enums, interfacess and types are in a API global context and should have names witch are 
     valid in the OTP scope to avoid misunderstanding and future conflicts when adding new types.
   - Types should have name describing the type, not the role it plays. 
   - Input types is often used in only one place. For none generic input types use names witch 
     contain both information about the relation and type, and avoid using "input" as a part of the
     name.
   - Prefer naming relationships by its role, over the target type. Both the GTFS and NeTEx 
     specifications are weak at this point, so consider NOT using the same names as in the 
     specifications. Do not use `Route.operator` but `Route.operatedBy`; Not `Stop.routes`, but 
     `Stop.servicingRoutes` or `Stop.visitingRoutes`.


### Design conventions/patterns

Below are some conventions/patterns we have decided on, there are of cause alternative 
ways to do these, but we want to be consistent.

### Set of values as a filter input

TODO - This need more analysis and discussion

```graphql
{
    plan (
        allowedAgencies: AllowedAndNotAllowedIds
        preferredAgencies: PreferredAndUnpreferredIds
    )
}

input AllowedAndNotAllowedIds @oneOf {
    # null mean ALL 
    allowed: [ID!]
    notAllowed: [ID!] = []
}

input PreferredAndUnpreferredIds {
    preferred: [ID!] = []
    unpreferred: [ID!] = []
}

## ALTERNATIVE

## Example query
{
    plan (
        routeRestrictions: {
            include: {
                # either **ids** or **not**, but not both
                ids: ["1", "2"]  # or
                not: ["2", "3"]
            }
            ## The agencies are here because they are used to filter the routes, if moved
            ## up one level this relationship is no longer clear, also using `agencies`
            ## here is probably wrong, but the role is not clearly defined; Hence difficult to 
            ## name properly - so we stick with agency.
            agencies: {
                include: {
                    ids: ["1", "2"] # or
                    not: ["2", "3"]
                }
                preferences: {
                    preferred: ["1", "3"]
                    unpreferred: ["2", "4"]
                }
            }
            preferences: {
                preferred: ["1", "3"]
                unpreferred: ["2", "4"]
            }
            otherThanPreferredPenalty: 100
            unpreferredPenalty: 400
        }
    )
}

## Types

input SetOfIds @oneOf {
    # null mean ALL 
    ids: [ID!]
    not: [ID!] = []
}

input PreferredUnpreferredIds {
    preferred: [ID!] = []
    unpreferred: [ID!] = []
}

input PlanAgencyRestrictions {
    include: SetOfIds
    preferences : PreferredUnpreferredIds
}

input PlanRouteRestrictions {
    include: SetOfIds
    agencies: PlanAgencyRestrictions 
    preferences : PreferredUnpreferredIds

    otherThanPreferredPenalty: Int = 100
    unpreferredPenalty: Int = 400
}

type Query {
    plan (routeRestrictions: PlanRouteRestrictions): Plan
}

``` 
