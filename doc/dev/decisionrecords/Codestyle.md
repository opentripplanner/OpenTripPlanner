# Code Style

We use the following code conventions for [Java](#Java) and [JavaScript](#JavaScript).

## Java

The OpenTripPlanner Java code style is revised in OTP v2.2. We use the
[Prettier Java](https://github.com/jhipster/prettier-java) as is. Maven is set up to
run `prettier-maven-plugin`. A check is run in the CI build, which fails the build preventing
merging a PR if the code style is incorrect.

There are two ways to format the code before checking it in. You may run a normal build with
Maven; it takes a bit of time, but it reformats the entire codebase. Only code you have changed
should be formatted, since the existing code is already formatted. The second way is to set up
Prettier and run it manually or hick it into your IDE, so it runs every time a file is changed.

### How to Run Prettier with Maven

Prettier will automatically format all code in the Maven "validate" phase, which runs before the
test, package, and install phases. So formatting will happen for example when you run:

```shell
% mvn test
```

You can manually run _only_ the formatting process with:

```shell
% mvn prettier:write
```

To skip the Prettier formating, use the profile `prettierSkip`:

```shell
% mvn test -P prettierSkip
```

To check for formatting errors, use the profile `prettierCheck`:

```shell
% mvn test -P prettierCheck
```

The check is run by the CI server and will fail the build if the code is incorrectly formatted.

### IntelliJ and Code Style Formatting

You should use the Prettier Maven plugin to reformat the code or run Prettier with Node (faster).

Prettier does _not_ format the doc and Markdown files, only Java code. So, for other files you
should use the _project_ code style. It is automatically imported when you first open the project.
But, if you have set a custom code style in your settings (as we used until OTP v2.1), then you need
to change to the _Project_ code style. Open the `Preferences` from the menu and select _Editor >
Code Style_. Then select **Project** in the \_Scheme drop down.

#### Run Prettier Maven Plugin as an External Tool in IntelliJ

You can run the Prettier Maven plugin as an external tool in IntelliJ. Set it up as an
`External tool` and assign a keyboard shortcut to the tool execution.

![External Tool Dialog](../images/ExternalToolDialog.png)

```text
Name:              Prettier Format Current File
Program:           mvn
Arguments:         prettier:write -Dprettier.inputGlobs=$FilePathRelativeToProjectRoot$
Working Directory: $ProjectFileDir$
```

> **Tip!**  Add an unused key shortcut to execute the external tool. Then you can use the old 
> shortcut to format other file types.

#### Install File Watchers Plugin in IntelliJ

You can also configure IntelliJ to run Prettier every time IntelliJ saves a Java file. But if you
are editing the file at the same time, you will get a warning that the file in memory and the file
on disk both changed, and asked to select one of them.

1. In the menu, open _Preferences..._ and select _Plugins_.
2. Search for "File Watchers" in the Marketplace.
3. Run _Install_.

##### Configure File Watchers

You can run Prettier upon every file save in IntelliJ using the File Watchers plugin. There are
several ways to set it up. Below is how to configure it using Maven to run the formatter. The Maven
way works without any installation of other components but might be a bit slow. So you might want to
install [prettier-java](https://github.com/jhipster/prettier-java/) in your shell and run it
instead.

```text
Name:              Format files with Prettier
File Type:         Java
Scope:             Project Files
Program:           mvn
Arguments:         prettier:write -Dprettier.inputGlobs=$FilePathRelativeToProjectRoot$
Working Directory: $ProjectFileDir$
```

### Other IDEs

We do not have support for other IDEs at the moment. If you use another editor and make one, please
feel free to share it.

### Sorting Class Members

Some of the classes in OTP have a lot of fields and methods. Keeping members sorted reduces merge
conflicts. Adding fields and methods to the end of the list will cause merge conflicts more often
than inserting methods and fields in an ordered list. Fields and methods can be sorted in "feature"
sections or alphabetically, but stick to it and respect it when adding new methods and fields.

The provided formatter will group class members in this order:

1. Getter and setter methods are kept together.
2. Overridden methods are kept together.
3. Dependent methods are sorted in breadth-first order.
4. Members are sorted like this:
    1. `static final` fields (constants)
    2. `static` fields (avoid)
    3. Instance fields
    4. Static initializers
    5. Class initializers
    6. Constructors
    7. `static` factory methods
    8. `public` methods
    9. Getter and setters
    10. `private`/package methods
    11. `private` enums (avoid `public`)
    12. Interfaces
    13. `private static` classes (avoid `public`)
    14. Instance classes (avoid)

### Javadoc Guidelines

As a matter of [policy](http://github.com/opentripplanner/OpenTripPlanner/issues/93), all new
methods, classes, and fields should include comments explaining what they are for and any other
pertinent information. For Java code, the comments should follow industry standards. It is best to
provide comments that explain not only *what* you did but also *why you did it* while providing some
context. Please avoid including trivial Javadoc or the empty Javadoc stubs added by IDEs, such as
`@param` annotations with no description.

- On methods:
    - Side effects on instance state (is it a pure function)
    - Contract of the method
        - Input domain for which the logic is designed
        - Range of outputs produced from valid inputs
        - Is behavior undefined or will the method fail when conditions are not met?
        - Are null values allowed as inputs?
        - Will null values occur as outputs (and what do they mean)?
    - Invariants that hold if the preconditions are met
    - Concurrency
        - Is the method thread-safe?
        - Usage constraints for multi-threaded use
- On classes:
    - Initialization and teardown process
    - Can an instance be reused for multiple operations, or should it be discarded?
    - Is it immutable, or should anything be treated as immutable?
    - Is it a utility class of static methods that should not be instantiated?

### Annotations

- On methods:
    - Method should be marked as `@Nullable` if they can return null values.
    - Method parameters should be marked as `@Nullable` if they can take null values.
- On fields:
    - Fields should be marked as `@Nullable` if they are nullable.

Use of `@Nonnull` annotation is not allowed. It should be assumed methods/parameters/fields are
non-null if they are not marked as `@Nullable`. However, there are places where the `@Nullable`
annotation is missing even if it should have been used. Those can be updated to use the `@Nullable`
annotation.

## JavaScript

As of [#206](https://github.com/opentripplanner/OpenTripPlanner/issues/206), we follow
[Crockford's JavaScript code conventions](http://javascript.crockford.com/code.html). Further
guidelines include:

* All .js source files should contain one class only
* Capitalize the class name, as well as the source file name (a la Java).
* Include the namespace definition in each and every file: `otp.namespace("otp.configure");`.
* Include a class comment. For example,

```javascript
/**
 * Configure Class
 *
 * Purpose is to allow a generic configuration object to be read via AJAX/JSON, and inserted into an
 * Ext Store
 * The implementation is TriMet route map-specific...but replacing ConfigureStore object (or member
 * variables) with another implementation will give this widget flexibility for other uses beyond
 * the iMap.
 *
 * @class
 */
```

> **Note:** There is still a lot of code following other style conventions, but please adhere to
> consistent style when you write new code, and help clean up and reformat code as you refactor.
