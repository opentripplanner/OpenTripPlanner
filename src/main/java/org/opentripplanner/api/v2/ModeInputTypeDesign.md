# Mode Input Types

The mode restriction input types are important to design well, because it is one of the most
frequently used consepts - both when constructing queries by hand and customizing the plan query
in the client.

Here is a UI example witch illustrate the problem well:
- [ ] Local trafic -- _All bus and tram route exect the school bus and local train_
- [ ] Regonal lines -- _Train except the local train_
- [ ] Air -- _All airplain routes_

Lets first express this as three sets A, B and C uning math set notations:
```
A := (BUS - SCHOOL_BUS) ∪ TRAM ∪ LOCAL_TRAIN
B := TRAIN - LOCAL_TRAIN
C := AIRPLANE
```
So we would like to create all 3 sets above as input objects and combine them to select any
combination of the three sets:

  `Ø` `A`, `B`, `C`, `A ∪ B`, `A ∪ C`, `B ∪ C`, `A ∪ B ∪ C`

We do not want to allow selecting nothing, it is not useful, so we interperate `Ø` as
everything.


## Discussion 

We do not want to implement set math in GraphQL, because that quickly become ugly. Instead, we can 
simplify and support most cases by:
 1. Let the user list all _allowed_ modes and submodes
 2. Then list all modes witch should be removed from the _allowed_ list
 3. Then allow for creating more than one set and take the union of these
 4. An empty set is not useful, so we interpret it as all elements
 5. Using *submodes* is a infrequent use case, and it should be easy to use it without.

Case 1 let us support the trivial case of including a set of modes/submodes. Case 2 let us 
remove elements from the total. This is especally important if we want for example *all modes exept
airlines*. New modes might be added in the future and _ALL - AIRLINES_ will then still give the 
right result. Case 3 make it simple for the client to have checkboxes for each group of modes and 
then submit any combination without doing the math to calculate the corret union.

The actual GraphQL types to achive the above and matches the examples below is not included here. 
See the schema for the acutal design.

## Examples

Lets try to express this with a few GraphQL examples for set `C` (simple), `B`, `A` and `A ∪ B ∪ C` 
(advanced). 


### Set `C` - All airplane routes

Selecting a singe mode should be simple:

```graphql
transitModes: [ { modes: [AIRPLANE] } ]
```
or simplified without the optional array brackets `[]`:
```graphql
transitModes: { modes: [AIRPLANE] }
```


### Set `B` - Train except the local train

```graphql
transitModes: {
    modes: [TRAIN]
    not: { submodes: [LOCAL_TRAIN] }
}
```


### Set `A` - All bus and tram route expect the school bus and local train

```graphql
transitModes: {
    modes: [TRAM, BUS]
    submodes: [LOCAL_TRAIN]
    not: { submodes: [SCHOOL_BUS] }
}
```


### Set `A ∪ B ∪ C` - The union of set A, B and C

We can do this in two equivalent ways, first as 3 elements. This is the three examples above listed
as three array elements, without any modifications:

```graphql
transitModes: [
    { modes: [AIRPLANE] },
    { modes: [TRAIN], not: { submodes: [LOCAL_TRAIN] } }
    { modes: [TRAM, BUS], submodes: [LOCAL_TRAIN], not: { submodes: [SCHOOL_BUS] } }
]    
```

We can also merge the 3 elements into a more compact format:

```graphql
transitModes: {
    modes: [AIRPLANE, BUS, TRAIN, TRAM],
    not: { submodes: [SCHOOL_BUS]
}
```
Note! How the `LOCAL_TRAIN` is removed above, because it was included in one set and excluded from
another. So, if a client do not want to perform the merge logic, then first approach is recommended.
For humans that need to type in the query, the second approach is probably the easiest.


### Set `Ø` - An empty set means ALL elements

Any of these are equivalent and will include all modes/submodes:
```graphql
transitModes: null
transitModes: []
transitModes: { modes: [] }
transitModes: { submodes: [] }
transitModes: { modes: [], submodes: [] }
```
Note! The following also include all elements, since we take the union of everything and all bus 
routes:
```
transitModes: [ {}, { mode: BUS }]
```

Including all modes is risky because a new mode can be added in the future. Be aware of this
and use the combinations that best fit with the use-case.

```graphql
# NOT REQOMENDED - This is currently all modes, but if a new mode is added in the future
#                - then it is not all modess any more
transitModes: { modes: [
AIRPLANE, BUS, CABLE_CAR, COACH, FERRY, FUNICULAR
GONDOLA, MONORAIL, RAIL, SUBWAY, TRAM, TROLLEYBUS
] }
```


## Exceptions - Error cases

It should be pretty hard to misuse this design, so we are not adding any checks for illegal 
combinations. Listing all modes/submodes in one of the sets are not recommended, but it is allowed.
