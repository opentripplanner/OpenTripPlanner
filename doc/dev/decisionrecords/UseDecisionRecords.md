# Decision Records

[TODO - Not precise] An OTP Decision Record is a justified software design choice that addresses a
functional or non-functional requirement that is significant. [Architectural Decision Records](https://adr.github.io/)
is a similar concept, but we have widened the scope to include any relevant decision about the
OTP development.

## Process

Decisions we make in the developer meetings are recorded in the [Developer Decision Records](/DEVELOPMENT_DECISION_RECORDS.md)
list. If the decision is small and uncontroversial, but yet important and can be expressed in 
maximum 2 sentences, we will list it here without any more documentation. If the decision requires
a bit more discussion and explanations, then we will create a PR with a document for it.

Use the **[template](/doc/dev/decisionrecords/_TEMPLATE.md) as a starting point for documenting
the decision.


### How to discuss and document a Decision Record

- Create a new pull-request and describe the decision record by adding a document to the 
  `/doc/dev/decisionrecords` folder. Use the [template](/doc/dev/decisionrecords/_TEMPLATE.md).
  template.
- Present the decision record in a developer meeting. Make sure to update the main description
  based on the feedback/discussion and decisions in the developer meeting.
- The final approval is done in the developer meeting, at least 3 developers representing 3
  different organisations should approve it. No vote against the proposal. If the developers
  are not able to agree, the PLC can decide.
- References to Architectural Decision Records in reviews can be done by linking or just typing
  e.g. `Use-Dependency-Injection` or [Use-Dependency-Injection](/DECISION_RECORDS.md#use-dependency-injection)

### Checklist
- [ ] Give it a meaningful title that quickly lets the reader understand what it is all about.
- [ ] Get it approved in a developer meeting with 3 votes in favor (3 organisations).
- [ ] Add the name and description to the list in the [Decision Records](/DECISION_RECORDS.md) list.
      Maximum two sentences should be used. Try to keep it as short as possible.
- [ ] Remember to link to the PR.
