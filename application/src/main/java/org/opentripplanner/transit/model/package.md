## Transit Model Design

TODO RTM - Add framework class diagram

### Services and its context

The `TransitService` is the main entry point for accessing all transit model objects. It may have 
nested services or provide read-only access to key model classes. For changing the model a 
`TransitEditorService` is created(obtained from the service). The editor can then be used to get a
builder for all _aggregate roots_[1]. For simplicity, we only allow _one_ editor to be active at 
any given time. 

[TODO RTM, clarify] Modifying, adding and deleting entities are **not** synchronized, so if more 
than one thread are doing updates at concurrent, then the synchronization responsibility is put on 
the client (graph builders and RealTimeSnapshot). When all modification are complete, then changes 
are made active by calling the `commit()` on the context. Before the `commit()` the changes are not 
visible by e.g. the routing. [TODO Diagram]


### Transit Entities

All transit entities must have an ID. Transit entities ar "root" level are considered _aggregate
roots_.


#### Non-null and nullable entity fields

Non-nullability of fields should be enforced in the Entity constructor by using `Objects.requireNonNull`,
`Objects.requireNonNullElse` or `ObjectUtils.ifNotNull`. We should enforce this for all fields 
required in both GTFS and in the Nordic NeTEx Profile. For enumeration types using a special value
like `UNKNOWN` is preferred over making the field optional.


#### sameAs(), equals() and hashCode()

`equals()` and `hashCode` uses type and `id` only, while `sameAs` is all fields including
`id`. The `id` is used for looking up entities in indexes and for references between entities, 
while `sameAs()` is used for modification detection.


#### logName() and toString()

We want a sensible toString() method implementation for all classes in the transit model. The
toString is used for testing, logging and debugging, and should not be used for any domain feature
or exposed on the APIs.

Note! The `toString()` should not include nested entities, this may lead to extensive logging and
cyclic references.

For all entities the `toString()` is implemented in `AbstractTransitEntity` in the following format:

```
SIMPLE_CLASS_NAME{ID LOG_NAME?}
```

The `LOG_NAME` is optional. An entity can implement `LogInfo` to provide extra information in the
`toString()` method. This is useful for including human recognizable information. The `LOG_NAME`
does not need to identify the entity uniquely.


### Value Objects

A value object is something whose equality is not based on identity(`id`). In theory enum classes 
is also value objects, but for simplicity we do not include them in this discussion.      

For value objects `equals()` and `sameAs()` should be equivalent. 

The `toString` should include all fields, unless this is too much, then only include enough 
information to identify the value object within the context it exists. 

Values object can **not** reference entities, but may reference nested value objects - avoid 
cyclic references.


### References

1. [Domain Driven Design](https://en.wikipedia.org/wiki/Domain-driven_design)