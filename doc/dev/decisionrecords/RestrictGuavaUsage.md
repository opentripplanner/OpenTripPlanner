# Restrict Guava Usage

Only Guava classes on the white-list enforced by the `GuavaArchitectureTest` may be used. Prefer the
JDK or the OTP utils where they provide an equivalent, and extend the white-list deliberately when
Guava is clearly the best option.

### Context

Guava is a very large general-purpose library, and much of it duplicates functionality that has
since been added to the JDK (e.g. `Preconditions` vs `Objects.requireNonNull`, `ImmutableList` vs
`List.of`/`List.copyOf`, `Iterables` vs the Stream API). Unrestricted use leads to two idioms for
the same thing, makes the code harder to read, and increases the cost of future library upgrades or
removal. The parts of Guava OTP genuinely benefits from are concentrated in a few areas — above all
the `Multimap` family, plus a handful of utilities like hashing.
