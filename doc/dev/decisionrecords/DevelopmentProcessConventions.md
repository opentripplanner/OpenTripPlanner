# Development Process Conventions

| Date       | People attending   | Status  |
|------------|--------------------|---------|
| 18.09.2025 | developer workshop | decided |


We need guidelines for the development process in general and the dev meeting in particular that enable us to better keep up with the increasing amount or pull requests. This list was agreed upon by the developers attending the OTP Summit in Helsinki in September 2025.

## Context and Problem Statement

Core developers experience an increasing workload doing reviews so that they don't find enough time for development themselves. The decisions made here aim to
- increase the quality of the PRs by catching potential problems early
- make dev-meetings more effective to potentially save time


## Decision and Consequences

The following list of actions are agreed upon by all attending developers. We will revisit this list in February 2026 to decide, if further actions are needed.

### 1. Time boxing discussions in dev-meetings
To avoid getting into detailed discussions that don't really lead anywhere, discussions in the dev meeting will be time boxed. If time is up, it will be decided if the discussion is beneficial to all people present and should be continued, or if the discussion should be continued in another manner. This could for example mean
- scheduling a design meeting
- specific people doing more research
- tabling the discussion for a later dev-meeting when people have had time to think about the problem more and/or when other key people are present

Time boxes:
- Agenda Item: 10 minutes
- PR: 5 minutes

Additionally, **no debugging** should be done in dev-meetings.


### 2. Only discuss PRs in dev-meetings if owner of PR is present
The author of the PR should be present to answer any questions and/or present their implementation. Exceptions to this rule are possible, for example when there are time zone constraints. Questions that are supposed to be discussed in the dev-meeting without the author being present should be outlined in a comment on the PR.

### 3. Only discuss draft PRs and issues in dev-meeting if they are on the agenda
To save time, we do not want to go through every issue and draft PR every dev-meeting. If a discussion on a specific draft PR or issue is needed, then it should be put onto the agenda.

### 4. Use threads in gitter
To avoid clutter in our Gitter channel, always use threads. Messages not abiding by this rule will be deleted.

### 5. Decide *early* if we want to invest resources into a PR made by non-core developers
All PRs made by non-core developers lead to the core developers having to spend resources on reviewing the code and potentially having to provide guidance to the author. We need to decide early, if the PR will be beneficial to enough contributors that investing these resources is worth it. This is especially important for PRs that will touch the core.

Product Owners should have the ultimate decision in this. That way the core developers will have the official backing of their employers and have fewer conflicts investing their time.

### 6. Issues will have to be categorized by setting a type
Every PR that isn’t a minor change should have a connected issue. 

All issues describing modifications to OTP need to be one of these types:
 - `Bug` Un unexpected problem or behaviour.
 - `Feature` A request, idea, or new functionality.
 - `Technical Improvement` None functional improvements like refactoring, optimizations, cleanup and CI/CD.

In case it's a minor change we set this label on the PR: `Minor Change`. The definition of something being a minor change is that there’s no need for preparing the change by reviewing the desing or the modification of OTP functionality. Documentation is often a minor change.


### 7. Approval of the different types of OTP modifications
For changes classified as `Bug` or `Technical Improvement`, approval of the architectural design is considered sufficient.

For changes classified as `Feature`, both the approval of that it is a desired change in OTP, and approval of the architectural design are required.

Tags we use for approval:
- `Design approved` Arcitectual design approved.
- `Feature approved` New functionality approved.
- `Feature rejected` New functionality rejected.

Modifications that are minor changes can be accepted without preparation, and will be added to OTP according to the rules for reviewing PRs.
