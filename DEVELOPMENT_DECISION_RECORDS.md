# Development Decision Records


## Use-Decision-Records

We will [use decision-records](doc/dev/decisionrecords/UseDecisionRecords.md) to document OTP 
development relevant decision. Use the [template](doc/dev/decisionrecords/_TEMPLATE.md) to describe
new decision records.


## Scout-Rule

Leave things BETTER than you found them, clean up code you visit or/and add unit
tests. Expect to include some code cleanup as part of all PRs.

## Follow-Naming-Conventions

Use established terminology from GTFS, NeTEx or the existing OTP code. Make sure the code is easy
to read and understand. [Follow naming conventions](doc/dev/decisionrecords/NamingConventions.md#naming-conventions) . 


## Write-Code-Documentation - Use JavaDoc

Document the business intention and decisions in the relevant code. Do not repeat the logic
expressed in the code. Use JavaDoc, you may have to refactor part of your code to encapsulate the
business logic into a method or class to do this. 

Document all `public` types, methods and fields with JavaDoc. It is ok to document implementation 
notes on `private` members and as inline comments.

> If you decide to NOT follow these decision records, then you must document why.

**See also**
 - [Developers-Guide &gt; Code comments](doc/user/Developers-Guide.md#code-comments).
 - [Codestyle &gt; Javadoc Guidelines](doc/dev/decisionrecords/Codestyle.md#javadoc-guidlines) - JavaDoc checklist


## Document-Config-and-APIs

Document API and configuration parameters.


## Respect-Codestyle

OTP uses prettier to format code. For more information on code style see the 
[Codestyle](doc/dev/decisionrecords/Codestyle.md) document.


## Use-Object-Oriented-Principals

Respect Object-Oriented principals
  - Honor encapsulation & Single-responsibility principle
  - Abstraction - Use interfaces when a module needs "someone" to play a role
  - Inheritance - Inheritances expresses “is-a” and/or “has-a” relationship, do not use it "just"
    to share data/functionality. 
  - Use polymorphism and not `instanceof`.


## Use-Dependency-Injection

Use dependency injection to wire components. You can use manual DI or Dagger. Put the 
wiring code in `<module-name>/configure/<Module-name>Module.java`.

OTP will use a dependency injection library or framework to handle object lifecycles (particularly
request-scoped vs. singleton scoped) and ensure selective availability of components, services,
context, and configuration at their site of use. Systems that operate via imperative Java code
(whether hand-written or generated) will be preferred over those operating through reflection or
external markup files. See [additional background](https://github.com/opentripplanner/OpenTripPlanner/pull/5360#issuecomment-1910134299).

## Use-Module-Encapsulation

Keep modules clean. Consider adding an `api`, `spi` and mapping code to
isolate the module from the rest of the code. Avoid circular dependencies between modules.


## DRY - Do not repeat yourself

Keep the code [DRY](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself) - Do not 
repeat yourself. Avoid implementing the same business rule in two places -> refactor.


## Avoid-Feature-Envy

[Feature envy](https://refactoring.guru/smells/feature-envy)


## Test-Coverage

All _business_ logic should have unit tests. Keep integration/system tests to a minimum. Add test at
the lowest level practical to test the business feature. Prefer unit tests on a method over a class,
over a module, over the system. On all non-trivial code, full _branch_ test coverage is preferable. 
Tests should be designed to genuinely demonstrate correctness or adherence to specifications, not 
simply inflate line coverage numbers.


## Use-Immutable-Types

Prefer immutable types over mutable. Use builders where appropriate. See 
[Records, POJOs and Builders](doc/dev/decisionrecords/RecordsPOJOsBuilders.md#records-pojos-and-builders)


## Be-Careful-With-Records

[Avoid using records if you cannot encapsulate it properly](doc/dev/decisionrecords/RecordsPOJOsBuilders.md#records)


