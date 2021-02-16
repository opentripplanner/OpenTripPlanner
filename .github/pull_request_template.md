## PR Instructions
When creating a pull request, please follow the format below. In each section, *replace* the guidance text with your own text. This "instructions" section and the guidance text should not be present in the submitted PR. These sections serve as a checklist - when you have replaced all of them with your own text, the PR is considered complete.

### Summary
Explain in one or two sentences what this PR achieves.

### Issue
Link to or create an [issue](https://github.com/opentripplanner/OpenTripPlanner/issues) that describes the relevant feature or bug. Add [GitHub keywords](https://help.github.com/articles/closing-issues-using-keywords/) to this PR's description (e.g., `closes #45`).
You need not create an issue for small bugfixes and code cleanups, but do describe the problem clearly in the "summary" section above.

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
