# Code Conventions

We try to follow these conventions or best practices. The goal is to get cleaner code and make the
review process easier:

- the developer knows what to expect
- the reviewer knows what to look for
- discussions and personal preferences can be avoided saving time
- new topics should be documented here

These conventions are not "hard" rules, and often there might be other forces witch pull a
decision in another direction, in that case documenting your choice is often enough to pass the
review.



## Best practices - in focus



- [ ] Document `public` interfaces, classes and methods - especially those part of a module api.
- [ ] Leave Things BETTER than you found them - clean up code you visit or/and add unit tests.
- [ ] [Feature envy](https://refactoring.guru/smells/feature-envy)
- [ ] Make types immutable if possible. References to other Entities might need to be mutable, if 
      so try to init them once, and throw an exception if set again. 
      Example:
```java 
   Builder initStop(Stop stop) { 
     if(this.stop != null) { throw new IllegalStateException("..."); } 
   }  
``` 


## Naming Conventions

### Methods

Here are a list of common prefixes used, and what to expect.

| Good method prefixes                                  | Description                                                                 |
|-------------------------------------------------------|-----------------------------------------------------------------------------|
| `stop() : Stop`                                       | Field accessor, equivalent to `getStop` as in the Java Bean standard        |
| `getStop(ID id) : Stop`                               | Get Stop by ID, throws exception if not found                               |
| `getStops(Collection<ID> id) : List/Collection<Stop>` | Get ALL Stops by set of IDs, throws exception if not found                  |
| `findStop(Criteria criteria) : Optional<Stop>`        | Find one or zero stops, return `Optional`                                   |
| `findStops(Criteria criteria) : List/Stream<Stop>`    | Find 0, 1 or many stops, return a collection or stream(List is preferred)   |
| `listStops() : List/Stream<Stop>`                     | List ALL stops in context, return a collection or stream(List is preferred) |
| `withStop(Stop stop) : Builder`                       | Set stop in builder, replacing existing value and return `this` builder     |
| `initStop(Stop stop) : void/Builder`                  | Set property ONCE, a second call throws an exception                        |
| `addStops(Collection<Stop> stops) : void/Builder`     | add set of stops to existing set, consider using a builder                  |


These prefixes are also "allowed", but not preferred - they have some kind of negative "force" to them.

| Ok method prefixes, but ...                 | Description                                                                                           |
|---------------------------------------------|-------------------------------------------------------------------------------------------------------|
| `withStops(Collection<Stop> stops) : this`) | Replace all stops in builder with new set, consider using `addStops(...)` instead                     |
| `addStop(Stop stop) : void`                 | Add a stop to collection, consider using a builder instead                                            |
| `setStop(Stop stop)`                        | Set a mutable stop reference. Avoid if not part of natural lifecycle. Use `initStop(...)` if possible |
| `getStop() : Stop`                          | Old style accessor, use the shorter form `stop() : Stop`                                              |


