# Developers Guide

## Contributing to the project

OpenTripPlanner is a community based open source project, and we welcome all who wish to contribute. There are several ways to get involved:

 * Join the [developer mailing list](http://groups.google.com/group/opentripplanner-dev)

 * Fix typos and improve the documentation on the wiki.

 * [File a bug or new feature request](http://github.com/openplans/OpenTripPlanner/issues/new).

 * Submit patches.  If you're not yet a committer, please attach patches to the relevant issue. A pull request for the relevant issue is probably the easiest way to do this.

 * Become a committer. If you'd like to contribute code to the project beyond the occasional patch, you can request commit rights for the project. Before doing so, you should:

  1. Make yourself known to the rest of the community by introducing yourself on the [mailing list](http://groups.google.com/group/opentripplanner-dev), hanging out in the IRC channel, and/or joining one of our weekly [[phone or irc meetings|Weekly-Check-In-Discussions]].

  2. Submit at least two patchs to the mailing list (or via a github pull request) for review. It doesn't have to be perfect, and it doesn't have be huge, but it should do something useful (e.g., fix a bug, add a new feature, make the code faster/more efficient, etc).  

### Tickets and checkins

All checkins should reference a specific ticket (decided in #175). For example, ` Fixes #9999 (spaceships aren't supported by routing engine) `. If no ticket exists for the feature or bug your code implements or fixes, you should [create a new ticket](http://github.com/openplans/OpenTripPlanner/issues/new) prior to checking in.

Github will automatically update tickets on checkin. If your commit message includes the text ` fixes #123 `, it will automatically append your message as a comment on the ticket and close it. If you simply mention ` #123 ` in your message, your message will be appended to the ticket but it will remain open.

Synonyms for closing tickets (append comment and close): 
```
fixes #xxx
fixed #xxx
fix #xxx
closes #xxx
close #xxx
closed #xxx
```

### Comments & code documentation

As a matter of [policy](http://github.com/opentripplanner/OpenTripPlanner/issues/93), all new methods, classes, and 
fields should include comments explaining what they are for and any other pertinent information. For Java code, 
the comments should use the [JavaDoc conventions](http://java.sun.com/j2se/javadoc/writingdoccomments). When 
documenting fields with getters and setters, the comments should go with the getter.

Most project documentation is now included directly in the OpenTripPlanner repository, rather than a separate wiki.
This allows version control to be applied to documentation as well as program source code. All pull requests that change
how OTP is used or configured should include changes to the documentation alongside code modifications.

### Date format

Please use only ISO 8601 date format (YYYY-MM-DD) on the wiki and throughout the project. This avoids the ambiguity 
that can result from differing local interpretations of date formats like 02/01/12. 

### Project proposals and decision making

Decisions are made by the OpenTripPlanner community through a proposal and informal voting process on the 
[project mailing list](http://groups.google.com/group/opentripplanner-dev).

While we do vote on proposals, we don't vote in a strict democratic sense, but rather as a way to easily register 
opinions, foster discussion, and move toward consensus. When responding to a proposal, we use the following system:

 * +1 - *I support this*

 * +0 - *I don't have a strong opinion, but I'm not opposed*

 * -0 - *I'm against this, but I don't have a good alternative / I'm not willing to do the work on the alternative / I won't block*

 * -1 - *Blocking no* (note: in general and when appropriate, this requires the blocker to propose something else that he/she would help put the time into doing)

A proposal does *not* need to be a formal or lengthy document; it can and should be a straightforward recommendation of 
what you want to do, ideally with a brief explanation for why it's a good idea. 

Proposals are just messages sent to the list and can be as simple as *"I think we should do X because of Y and Z. 
Deadline for response is 10/29. Assuming I've heard no blocking votes by then, I'll go ahead."* 
Note that you should make sure to include a **deadline** by which you will go ahead and do what you're proposing 
if you don't hear any blocking responses. In general, you should leave at least 72 hours for people to respond. 
This is not a hard-and-fast rule and you should use your best judgement in determining how far in the future the 
deadline should be depending on the magnitude of the proposal and how much it will effect the overall project and the 
rest of the community.

Of course you may always fork the [OTP repo on GitHub](https://github.com/opentripplanner/OpenTripPlanner/) 
and submit your changes as a pull request.

## Code style

### Java

OpenTripPlanner uses the same code formatting and style as the [GeoTools](http://www.geotools.org/) and 
[GeoServer](htp://geoserver.org) projects. It's a minor variant of the 
[Sun coding convention](http://www.oracle.com/technetwork/java/codeconv-138413.html). Notably, **we do not use tabs** 
and we allow for lines up to 100 characters wide. 

The Eclipse formatter config supplied by the GeoTools project allows comments up to 150 characters wide. 
A modified version included in the OpenTripPlanner repository will wrap comments to the same width as lines of code, 
which makes for easier reading in a narrow window.

If you use Eclipse, you should do the following to make sure your code is automatically formatted correctly:

1. Open the project `Properties` (right-click on the project directory in Eclipse and select `Properties` or choose `Project` -> `Properties`).

2. Select `Java`, then `Code Style`, and finally `Formatter`.  

3. Check the `Enable project specific settings` checkbox.

4. Click `Import...`, select the `formatter.xml` file in the root of the OpenTripPlanner git repository, and click `Open`.

5. Click `OK` to close the `Properties` window.


### JavaScript

As of #206, we follow [Crockford's JavaScript code conventions](http://javascript.crockford.com/code.html). Further guidelines include:

 * All .js source files should contain one class only

 * Capitalize the class name, as well as the source file name (a la Java)

 * Include the GNU LGPL header at top of file, i.e., `/* This program is free software:...*/`

 * Include the namespace definition in each and every file: `otp.namespace("otp.configure");`

 * Include a class comment. For example,                                                                                                      

```java
    /**
     * Configure Class
     *
     * Purpose is to allow a generic configuration object to be read via AJAX/JSON, and inserted into an Ext Store
     * The implementation is TriMet route map specific...but replacing ConfigureStore object (or member variables) with
     * another implementation, will give this widget flexibility for other uses beyond the iMap.
     *
     * @class
     */
```

*Note: There is still a lot of code following other style conventions. We are reformatting as we write new code and refactor.*

