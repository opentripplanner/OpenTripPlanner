## PR Instructions

When creating a pull request, please follow the format below. For each section, *replace* the
guidance text with your own text, keeping the section heading. If you have nothing to say in a
particular section, you can completely delete the section including its heading to indicate that you
have taken the requested steps. None of these instructions or the guidance text (non-heading text)
should be present in the submitted PR. These sections serve as a checklist: when you have replaced
or deleted all of them, the PR is considered complete. As of 2021, most regular OTP contributors
participate in our twice-weekly conference calls. For all but the simplest and smallest PRs,
participation in these discussions is necessary to facilitate the review and merge process. Other
developers can ask questions and provide immediate feedback on technical design and code style, and
resolve any concerns about long term maintenance and comprehension of new code.

### Summary

Explain in one or two sentences what this PR achieves.

### Issue

Link to or create an [issue](https://github.com/opentripplanner/OpenTripPlanner/issues) that
describes the relevant feature or bug. You need not create an issue for small bugfixes and code
cleanups, but in that case do describe the problem clearly and completely in the "summary" section
above. In the linked issue (or summary section for smaller PRs) please describe:

- Motivation (problem or need encountered)
- How the code works
- Technical approach and any design considerations or decisions

Remember that the PR will be reviewed by another developer who may not be familiar with your use
cases or the code you're modifying. It generally takes much less effort for the author of a PR to
explain the background and technical details than for a reviewer to infer or deduce them. PRs may be
closed if they or their linked issues do not contain sufficient information for a reviewer to
proceed.

Add [GitHub keywords](https://help.github.com/articles/closing-issues-using-keywords/) to this PR's
description, for example:

Closes #45

### Unit tests

Write a few words on how the new code is tested.

- Were unit tests added/updated?
- Was any manual verification done?
- Any observations on changes to performance?
- Was the code designed so it is unit testable?
- Were any tests applied to the smallest appropriate unit?
- Do all tests
  pass [the continuous integration service](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/doc/user/Developers-Guide.md#continuous-integration)
  ?

### Documentation

- Have you added documentation in code covering design and rationale behind the code?
- Were all non-trivial public classes and methods documented with Javadoc?
- Were any new configuration options added? If so were the tables in
  the [configuration documentation](Configuration.md) updated?

### Changelog

The [changelog file](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/doc/user/Changelog.md)
is generated from the pull-request title, make sure the title describe the feature or issue fixed.
To exclude the PR from the changelog add the label `+Skip Changelog` to the PR.

### Bumping the serialization version id

If you have made changes to the way the routing graph is serialized, for example by renaming a field
in one of the edges, then you must add the label `+Bump Serialization Id` to the PR. With this label
Github Actions will increase the field `otp.serialization.version.id` in `pom.xml`.
