# Codestyle

We use the following code conventions for [Java](#Java) and [JavaScript](#JavaScript).

## Java
The OpenTripPlanner Java code style is revised in OTP2. We use the Google Java style guide, with a 
few modifications. Here is the original Google style guide: 
https://google.github.io/styleguide/javaguide.html

### IntellJ Code Style formatter 
If you use IntelliJ, import the provided [intellij-code-style.xml](https://github.com/opentripplanner/OpenTripPlanner/blob/2.0-rc/intellij-code-style.xml). 
Open the `Preferences` from the menu and select _Editor > Code Style_. Then import the code-style
xml document. Configure _Scheme_ using  _Import Scheme > IntelliJ IDEA code style XML_.

Note that the IntelliJ formatter will not always format the code according to the coding standard.
It struggle to do the line breaks properly. Hence, use it to format new code and to rearrange 
members, then manually fix any mistakes.

### Other IDEs
We do not have support for other IDEs at the moment. If you use another editor and make one please
feel free to share it.  

### Code Style

Our style differs in the following ways from the Google guide. Each point references the section of
the original document that is modified.

  - 4.5.1 We apply the same line breaking rules to parentheses as to curly braces. Any time the
    contents of the parentheses are not all placed on one line, we insert a line break after the
    opening paren, and place the closing one at the beginning of a new line. 
    ```java
    // 
    int value = calculateSomeValue(
        arg1, 
        arg2, 
        arg3
    );

    // Also Ok
    int value = calculateSomeValue(
        arg1, arg2, arg3 
    );

    // Avoid this
    int value = calculateSomeValue(arg1,
        arg2, arg3 
    );
    // and this
    int value = calculateSomeValue(
        arg1, arg2, arg3) 
        
    ```
    
  - 4.6.1 We insert double empty lines before comments that introduce the highest level groupings of
    methods or fields within a class, for example `/* private methods */`, 
    `/* symbolic constants */`. 
    ```
    public foo() { 
    }
    
    
    /* private methods */
    
    private bar() ...
    ```
  - 4.8.3 the final example is not allowed. All opening brackets or parens should be on the same
    line as the identifier or other construct that they follow.
  - 4.8.5 All annotations on classes, fields, and methods should always have a newline after the
    last annotation, i.e. they should not appear on the same line as the identifier they annotate,
    and should only appear on the same line as other annotations. Series of multiple annotations may
    each appear on a separate line, or may all be grouped together on the same line.
  - 4.8.6.1 On multi-line `/* ... */` comments we do not begin the intermediate lines with asterisks
    `*`.
  - 5.2.8 We only use single capital letters (single characters) for generic type parameters.
  - 7.2 We do not begin Javadoc with summary fragments. This is because will no longer generate and
    publish Javadoc pages, the Javadoc will only be used within IDEs.
  - 7.3.1 The item in the original document implies that trivial Javadoc like `/** Returns the 
    canonical name. */` should still be included. There is almost always something more to explain
    to someone who is seeing this method or class for the first time.

### Notes on breaking lines
The eye scan the code much faster if there is less need of horizontal movement, so formatting the 
code becomes a balance on how long the lies should be and how to break it. Try to brake the code
at the outer-most scope aligning expressions with the same scope. Consider to chop down all 
expressions with the same scope and indent them, do not align code further to the right than the
indentation margin.
```
  // Conider this code:
  xxxx xxx = xxx + xxx * xxx - ( x.xxxx().xx().xxx() - xxx ) / xxx;

  // Break tha line as every operator, pharenphasis and method chanin.
  // This is a bit extrem, but illustrates the correct way to break the lines.
  xxxx xxx 
      = xxx 
      + 
          xxx 
          * xxx 
      - 
          ( 
              x.xxxx()
                  .xx()
                  .xxx() 
              - xxx 
          ) 
          / xxx
      ;

  // Prefered compromize
  xxxx xxx = xxx + xxx * xxx 
      - ( x.xxxx().xx().xxx() - xxx ) / xxx

  // or 
  xxxx xxx 
      = xxx + xxx * xxx 
      - ( 
          x.xxxx().xx().xxx() 
          - xxx 
      ) / xxx
```
```
  // Right alignment not allowed
  xxxx xxx = xxx 
           + xxx * xxx; 
  
  // use indentation margin instead
  xxxx xxx = xxx
      + xxx * xxx; 

```

### Sorting class members
Some of the classes in OTP have a lot of fields and methods. Keeping members sorted reduce the
merge conflicts. Adding fields and methods to the end of the list will cause merge conflicts 
more often than inserting methods and fields in an ordered list. Fields and methods can be sorted
in "feature" sections or alphabetically, but stick to it and respect it when adding new methods 
and fields.

The provided formatter will group class members in this order:

  1. Getter and Setter methods are kept together
  2. Overridden methods are kept together
  3. Dependent methods are sorted in a breadth-first order.
  4. Members are sorted like this:
     1. `static` `final` fields
     2. `static` fields
     3. `static` initializer
     4. `final` fields
     5. fields
     6. class initializer (avoid using it)
     7. Constructor
     8. `static` methods
     9. `static` getter and setters
     10. methods
     11. getter and setters
     12. enums
     13. interfaces
     14. `static` classes
     15. classes
  5. Each section of members are sorted by visibility:
     1. ´public´
     2. package private
     3. ´protected´
     4. ´private´
   

### JavaDoc Guidlines

What to put in Javadoc:
- On methods: 
  - Side effects on instance state (is it a pure function)
  - Contract of the method
    - Input domain for which the logic is designed
    - Range of outputs produced from valid inputs
    - Is behavior undefined or will fail when conditions are not met
    - Are null values allowed as inputs
    - Will null values occur as outputs (what do they mean)
  - Invariants that hold if the preconditions are met
  - Concurrency
    - Is method thread-safe
    - Usage constraints for multi-threaded use
- On classes:
  - Initialization and teardown process
  - Can instance be reused for multiple operations, or should it be discarded
  - Is it immutable or should anything be treated as immutable
  - Is it a utility class of static methods that should not be instantiated
  
## JavaScript
  
As of #206, we follow [Crockford's JavaScript code conventions](http://javascript.crockford.com/code.html). Further guidelines include:
  
  * All .js source files should contain one class only
  * Capitalize the class name, as well as the source file name (a la Java)
  * Include the namespace definition in each and every file: `otp.namespace("otp.configure");`
  * Include a class comment. For example,                                                                                                      
  
```javascript
/**
 * Configure Class
 *
 * Purpose is to allow a generic configuration object to be read via AJAX/JSON, and inserted into an
 * Ext Store
 * The implementation is TriMet route map specific...but replacing ConfigureStore object (or member
 * variables) with another implementation, will give this widget flexibility for other uses beyond 
 * the iMap.
 *
 * @class
 */
```
  
*Note: There is still a lot of code following other style conventions, but please adhere to 
consistent style when you write new code, and help clean up and reformat code as you refactor.*
