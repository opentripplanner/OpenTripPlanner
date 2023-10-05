# Architectural Decision Records (ADRs)

An Architectural Decision (AD) is a justified software design choice that addresses a functional or
non-functional requirement that is architecturally significant. ([adr.github.io](https://adr.github.io/))

## Process

Architectural decisions we make in the developer meetings are recorded here. If the decision is 
small and uncontroversial, but yet important and can be expressed in maximum 2 sentences, we will 
list it here without any reference. If the decision require a bit more discussion and explanations
an issue on GitHub should be created - use the template below.

### How to discuss and document an Architectural Decision

 - [Create a new issue](https://github.com/opentripplanner/OpenTripPlanner/issues/new/choose?template=adr.md) 
using the [ADR.md](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/ISSUE_TEMPLATE/ADR.md)
template.
 - Make sure to update the main description based on the feedback/discussion and decisions in the 
   developer meeting.
 - The final approval is done in the developer meeting, at least 3 developers representing 3 
   different organisations should approve - and no vote against the proposal. If the developers
   are not able to agree the PLC can decide.
 - Add the ADR to the list below. Maximum two sentences should be used - try to keep it as short as 
   possible. Remember to link to the discussion.
 - References to Architectural Decision Records in reviews can be done by linking or just typing 
   e.g. `ADR2`. Use the `[ADR1]()`


## Records

### ADR-0 Scout rule
Leave Things BETTER than you found them - clean up code you visit or/and add unit
tests. Expect to include some code cleanup as part of all PRs.

### ADR-1 Naming
[Follow naming conventions](CODE_CONVENTIONS.md#naming-conventions) . Make sure the 
code is easy to read and understand.

### ADR-2 Code documentation - JavaDoc
Document the business intention and decisions in the relevant code. Do not repeat the logic
expressed in the code. The prefered way is to use JavaDoc, you may have to refactor part of your
code to encapsulate the business logic into a method or class to do this. 

> If you decide to NOT follow an Architectural Decision, then expect to document why.

**See also**
 - [Developers-Guide &gt; Code comments](docs/Developers-Guide.md#code-comments).
 - [Codestyle &gt; Javadoc Guidlines](docs/Codestyle.md#javadoc-guidlines) - JavaDoc checklist

### ADR-3 Code style
OTP uses prettier to format code. For more information on code style see the 
[Codestyle](docs/Codestyle.md) document.

### ADR-4 OOP
Respect Object-Oriented principals
  - Honor encapsulation & Single-responsibility principle
  - Abstraction - Use interfaces when a module need "someone" to play a role
  - Inheritance - Inheritances expresses “is-a” and/or “has-a” relationship, do not use it "just"
    to share data/functionality. 
  - Use polymorphism and not `instanceof`.

### ADR-5 Dependency injection
Use dependency injection to wire components. You can use manual DI or Dagger. Put the 
wiring code in `<module-name>/configure/<Module-name>Module.java`.

### ADR-6 Module encapsulation
Keep modules clean. Consider adding an `api`, `spi` and mapping code to
isolate the module from the rest of the code. Avoid circular dependencies between modules.

### ADR-7 JavaDoc
Document all `public` types, methods and fields.

### ADR-8 API doc
Document API and configuration parameters.

### ADR-9 DRY
Keep the code [DRY](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself) - Do not 
repeat yourself. Avoid implementing the same business rule in two places -> refactor.

### ADR-10 Feature envy
[Feature envy](https://refactoring.guru/smells/feature-envy)

### ADR-11 Test coverage
All business logic should have unit tests. Keep integration/system tests to a
minimum. Add test at the lowest level practical to test the business feature. Prefer unit tests on
a method over a class, over a module, over the system.

### ADR-12 Immutable types
Prefer immutable types over mutable. Use builders where appropriate. See 
[Records, POJOs and Builders](CODE_CONVENTIONS.md#records-pojos-and-builders)

### ADR-13 Records
[Avoid using records if you can not encapsulate it properly](CODE_CONVENTIONS.md#records)

