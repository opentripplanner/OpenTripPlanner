### Transit Entities

All transit entities must have an ID. Transit entities ar "root" level are considered _aggregate
roots_.


#### @Nonnull and @Nullable entity fields

All fields getters(except primitive types) should be annotated with `@Nullable` or `@Nonnull`. None
null field should be enforced in the Entity constructor by using `Objects.requireNonNull`,
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

A value object is something whose equality is **not** based on identity(`id`). In theory enum classes
is also value objects, but for simplicity we do not include them in this discussion.

For value objects `equals()` and `sameAs()` should be equivalent.

The `toString` should include all fields, unless this is too much, then only include enough
information to identify the value object within the context it exists.

Values object can **not** reference entities, but may reference nested value objects - avoid
cyclic references.


### References

1. [Domain Driven Design](https://en.wikipedia.org/wiki/Domain-driven_design)