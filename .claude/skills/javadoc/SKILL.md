---
name: javadoc
description: Write or review JavaDoc for Java code in OTP. Use when adding or changing public types, methods or fields, or when asked to write, improve or review JavaDoc/comments in a diff.
---

# JavaDoc style for OTP

Write JavaDoc that tells the reader what the code cannot. Document the business intention, not the
mechanics. If the doc only restates the name or signature, delete it or rewrite it.

## Scope

- With no argument, apply to the JavaDoc in the current diff (`git diff` plus staged changes).
- With a file, class or package argument, apply to that target.
- Write JavaDoc for **public** types, methods and fields. Package-private and private members only need
  it when the intent is not obvious.
- For properties, put the JavaDoc on the get accessor `arrivalTime()`, not on the setter
  `arrivalTime(int time)` or the field. Use the setter only when there is no get accessor, and the
  field only when there is neither.
- Edit only JavaDoc and comments. Do not change code behavior.

## Rules

1. **Summary sentence first.** One sentence on what the element is *for*, in business terms. Start with a noun phrase
   for types and fields, a verb for methods. Do not repeat the name ("Gets the foo" on `getFoo()` is noise).
2. **Say what is not obvious.** Prefer these, when they apply:
   - why it exists, or which problem it solves
   - invariants, ordering and uniqueness guarantees
   - units and ranges (seconds vs. milliseconds, inclusive vs. exclusive bounds). But if all times inside a module 
     are in seconds, like in Raptor, then it should not be repeated. It is enough to say it once. 
   - nullability and the meaning of "empty"
   - thread-safety and mutability
   - performance constraints (Raptor hot paths) the caller must respect
3. **Do not describe the implementation.** No "loops over", "calls", "uses a map". If the how matters to a caller,
   state it as a guarantee.
4. **`@param` / `@return` only when they add information.** Drop tags that restate the
   signature. Do not write `@param x the x`.
5. **Use `{@link}` and `{@code}`** for related types, methods and literals. Link to the abstraction a reader should use
   instead, when one exists. Avoid using `{@link}` if the target is not imported.
6. **Use `<p>` between paragraphs** and `<ul>` for lists. Keep the doc short; two or three paragraphs is a ceiling for
   most elements.
7. **Wrap at 100 characters**, matching the code. Fill each line up to the limit; do not break a
   line early when the next word still fits. Prettier does not reflow JavaDoc, so wrap by hand.
8. **Plain, present-tense, active voice.** No filler ("This method is used to"), no marketing words,
   no emojis.
9. **Define each concept once, at the highest level it belongs to (DRY).** The code has a natural
   hierarchy: module, package, interface or owning type, implementation, leaf. Define terminology
   in one place near the top and use it without documentation further down.
   - Before documenting an element, grep for the concept's name (e.g. `iterationDepartureTime`)
     and find where it is already defined: usually the interface, the owning type, or the
     package's `package.md`/`package-info.java`.
   - If a definition exists, do not re-document it. Leaf elements (implementations, overrides,
     accessors, builders, test helpers, records whose component names say it all) get no
     JavaDoc, or one short sentence with a `{@link}` to the definition. JavaDoc is inherited by
     overriding methods, so do not copy it. Use `{@inheritDoc}` only to add to the parent doc.
   - If no definition exists, write it at the top of the hierarchy, not in the leaf you are
     looking at.
   - This applies to the semantics of a concept as well as to units and slack rules. State a
     module-wide convention (such as "all times are in seconds") once, at module or package level.
10. **Inline comments are rare.** Keep one only for a non-obvious "why". Delete comments that narrate the code.
11. **Keep docs true.** When code changes, update its JavaDoc in the same edit. Remove stale references to renamed
    or deleted elements.

## Process

1. Read the target code and its callers or tests when the intent is unclear. If the intent cannot be found in the code,
   ask the user. Do not invent a rationale.
2. Read two or three neighboring classes in the same package and match their tone and density.
3. Locate the owning type or interface of each concept in the target and check whether its
   definition already exists above it. Move or remove duplicated definitions toward the top.
4. Write or revise the JavaDoc following the rules above.
5. Check every JavaDoc line you wrote against Rule 7.
6. Report briefly: which elements were documented, which duplicates were removed or replaced by a
   link, and any where you were unsure of the intent.

## Example

Weak:

```java
/**
 * Gets the arrival time.
 *
 * @return the arrival time
 */
int arrivalTime();
```

Better, defined once on the interface that owns the concept (the module states that all times are
seconds since the start of the service day, so the unit is not repeated):

```java
/**
 * The time the passenger arrives at this stop. Includes the alight-slack of the previous trip.
 */
int arrivalTime();
```

Leaf, an implementation of the same interface. No JavaDoc: the interface already defines it.

```java
@Override
public int arrivalTime() {
  return arrivalTime;
}
```

When a leaf needs a pointer, for example a field with a different name:

```java
/** The arrival time, see {@link StopArrival#arrivalTime()}. */
private final int time;
```