# OTP Sandbox Extensions

- The sandbox is a place to test and implement new "experimental" features.
- This should not be used for bug fixes and smaller changes.
- Consider forking if the feature is valuable to one deployment only.

## Terminology

**Main/core**   -- All OTP code and additional files, NOT part of the sandbox.
(`docs`, `src/main`, `src/test` and so on)

**Extensions** -- All features implemented in the OTP Sandbox, provided with no guarantees.
(`src/ext`, `src/ext-test`)

## Sandbox Goals

- Reduce work for PR approval
- Allow experimental code to evolve (in a Sandbox)
- Encourage refactoring and creation of extension points in the main code.
- Increase visibility and cooperation of development of new features.
- Feature toggle
    - Sandbox features should use the _OTPFeature_ to enable the code. Sandbox features are by
      default off. To toggle features on/off se the [configuration documentation](Configuration.md).

## Contract

- Give your feature a name: `<extension name>`
- A new feature is isolated from the rest of the code by putting it in the directory `src/ext`. Java
  code should have package prefix `org.opentripplanner.ext.<extension name>`. Unit tests should be
  added in the test directory: `src/ext-test`
- To integrate the new feature into OTP you may have to create new extension points in the main/core
  code. Changes to the core OTP are subject to normal a review process.
- Create a readme file (`docs/sandbox/<Extension Name>.md` package including:
    - Extension Name
    - Contact info
    - Change log
    - Documentation of the feature (optional)
- List your extension in the [Available extensions](#Available extensions) section and in the
  [mkdocs config file](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/mkdocs.yml).
- Use feature toggling to enable a feature at runtime. The feature must be disabled by default. A
  feature is toggled _on_ using the config files.
- Only code modifying the main code(`src/main`, not `src/ext`) is reviewed. The current coding
  standard apply to the extension code as well - but the code is not necessarily reviewed.
- There are no grantees - the authors of an extension can change its API any time they want.
- Anyone can request the feature to be merged into the main code. An approval from the PLC and a new
  review is then required. The reviewers may request any changes, including API changes.
- If an extension is taken into the core/main OTP code, any API included may change, no BACKWARD
  compatibility is guaranteed. I.e. the reviewers may require changes before it is merged.
- The feature submitters is responsible for maintaining and testing the extension code, but do not
  need to provide any guarantees or support. If the extension is merged into the main code the
  author will in fact need to provide support and maintenance.
- When someone at a later point in time want to change the main code the only thing they are
  responsible for - with regard to the extension code - is:
    - that it compiles.
    - that the unit tests run. If a test is not easy to fix, it can be tagged with @Ignore. If
      ignored it would be polite to notify the author.
- Changes to the main OTP API that cannot be toggled _in_ must be clearly marked/tagged as part of
  an experimental feature and documented - This code is subject to review.
- If a feature is old and not maintained it can be removed 1 month after notifying the submitter
  (using contact info in README file).
- Introducing new dependencies needs approval. They are NOT approved if they are likely to be a
  maintenance challenge (many transitive dependencies or potential conflicts with other
  versions/libraries).


