## PR Instructions
When creating a pull request, please follow the format below. For each section, *replace* the guidance text with your own text, keeping the section heading. If you have nothing to say in a particular section, you can completely delete the section including its heading to indicate that you have taken the requested steps. None of these instructions or the guidance text (non-heading text) should be present in the submitted PR. These sections serve as a checklist: when you have replaced or deleted all of them, the PR is considered complete.

### Summary
Explain in one or two sentences what this PR achieves.

### Issue
Link to or create an [issue](https://github.com/opentripplanner/OpenTripPlanner/issues) that describes the relevant feature or bug. 
You need not create an issue for small bugfixes and code cleanups, but do describe the problem clearly in the "summary" section above.
Add [GitHub keywords](https://help.github.com/articles/closing-issues-using-keywords/) to this PR's description, for example:

`closes #45`

### Unit tests
Write a few words on how the new code is tested. 
- Were unit tests added/updated?
- Was any manual verification done?
- Any observations on changes to performance?
- Was the code designed so it is unit testable?
- Were any tests applied to the smallest appropriate unit?
- Do all tests pass [the continuous integration service](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/docs/Developers-Guide.md#continuous-integration)?

### Code style
Have you followed the [suggested code style](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/docs/Developers-Guide.md#code-style)? 

### Documentation
- Have you added documentation in code covering design and rationale behind the code?
- Were all non-trivial public classes and methods documented with Javadoc?
- Were any new configuration options added? If so were the tables in the [configuration documentation](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/docs/Configuration.md) updated?

### Changelog
Was a bullet point added to the [changelog file](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/docs/Changelog.md) with description and link to the linked issue?
