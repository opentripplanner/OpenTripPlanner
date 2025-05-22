# Analysis and design

We want to get better at design, so we want some documentation up front for large changes. If a 
problem is complex, changes the core OTP model, and/or the API significantly, then the developer
requesting a change should be prepared to provide some design documentation.

The analysis should be started and at least mapped out in an issue. The design can be documented 
in the issue or the PR. 


## Analysis & Design

A discussion in a developer meeting is usually a good point to start. 

- Ask what is expected?
- Diagrams beat words in most cases, and help focus on the problem - not implementation details.


### Artifacts

We usually do not require a long list of requirements and analyses documentation. But these 
artifacts may help, but none of these are required. Ask in the developer meeting what to expect. 

 - [ ] Summarise the discussion in the developer meeting in the issue or PR.
 - [ ] List use-cases, one sentence per use-case is often enough.
 - [ ] In/out matrix, list features you are NOT planning to implement.
 - [ ] Draw diagrams
   - [ ] **Domain model** — If the core model or APIs are significantly changed
   - [ ] State diagram
   - [ ] Collaboration diagram

We recommend using [draw.io](https://www.drawio.com/). It is free and available as an Intellij 
plugin (Diagrams.net), web application and desktop application.


## Domain model

A domain model focusses on the language and the relationships. All implementation details can be left 
out. Details which is not relevant for the problem you are solving can also be left out, focus on 
the elements which helps understand the problem. Use plain english and not tech to describe 
the model. For example, only listing the field name, and not the type is ok if the type is obvious.

Notation is not important, but try to follow the UML syntax below. You may use more advanced UML 
syntax if you want, but keep in mind that you should be able to use the diagram to discuss the 
problem with a non-developer. A product-owner or other person who knows the domain should with 
a little help be able to understand the main parts of the drawing.

When doing review, focus on the domain problem, not syntax — ask about things you do not understand.


### Domain model - notation cheat sheet

![Domain Model Notation](../images/DomainModelNotation.svg)

