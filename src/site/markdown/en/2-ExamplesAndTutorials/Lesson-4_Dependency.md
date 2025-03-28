# Lesson 3: Dependency Handling (`DEPENDENCY_BASED` Execution Mode)

If we write down rules applied to resolve what scenes and in what order they are executed in an imperative way, it will
as follows:

1. Explicitly declared methods are executed in the declared stage.
2. Implicit execution never happens when `PASSTHTHROUGH` mode is selected.
3. In `DEPENDENCY_BASED` mode, following rules are respected.
    1. For `@DependsOn`:
        1. Implicit execution happens in `beforeAll` stage.
        2. Implicit execution happens at most only once per scene.
           This means when multiple scenes depend on the same single scene, the scene depended on will be executed only
           once.
        3. In implicit scene execution, the order of the explicit scenes that depend on implicit scenes is respected.
           That is, topological sort is applied to the dependency graph composed of the scene methods and scenes not
           explicit declared will be put in the beginning of `beforeAll` stage.
    2. For `@When`:
        1. Implicit execution happens only when the scene mentioned by the `@When` is explicitly specified.
        2. Implicit execution happens right after the scene mentioned by the `@When` annotation.
    3. For `@PreparedBy`:
        1. Implicit execution happens only when it is the first `@PreparedBy` annotation for the scene or all the
           previous ones failed.
        2. After one sequence defined by `@PreparedBy` has succeeded the scene itself will be performed.
           If the scene succeeds, the rest scenes provided by `@PreparedBy` will not be performed.
        3. When the previous scene or any of `@PreparedBy` scene fails, the next one will be tried.
           If it is the last one, the entire scene considered failed.
    4. For `@ClosedBy`:
        1. This is only valid for scenes in `beforeAll` and `beforeEach` stages.
        2. If a scene annotated with this in `beforeAll` stage succeeds, the scene specified by this annotation will be
           attempted in `afterAll` stage.
        3. If it is in `beforeEach` stage, the specified scene will be attempted in `afterEach` stage.

Don't get scared.
This looks a bit complex but just carefully designed to model what human does a manual test.
In this section we will go over the annotations mentioned in the rules above one by one with working examples.

## `@DependsOn`: The Simple Dependency

`@DependsOn` used to declare regular dependencies.

<!-- @formatter:off -->
> 3. In `DEPENDENCY_BASED` mode, following rules are respected.
>     1. For `@DependsOn`:
>         1. Implicit execution happens in `beforeAll` stage.
>         2. Implicit execution happens at most only once per scene.
>            This means when multiple scenes depend on the same single scene, the scene depended on will be executed only
>            once.
>         3. In implicit scene execution, the order of the explicit scenes that depend on implicit scenes is respected.
>            That is, topological sort is applied to the dependency graph composed of the scene methods and scenes not
>            explicit declared will be put in the beginning of `beforeAll` stage.
<!-- @formatter:on -->

Following is the usage of the annotation.:

```java

@AutotestExecution(defaultExecution = @Spec(
    value = "sceneMethod",
    planExecutionWith = DEPENDENCY_BASED))
public class Lesson extends LessonBase {
  @Named
  public Scene setUpMethod() {
    return Scene.begin()
                .act(new Let<>("InsDog"))
                .act(new Sink<>(System.out::println))
                .end();
  }
  
  @DependsOn("setUpMethod")
  @Named
  public Scene sceneMethod() {
    return Scene.begin()
                .act(new Let<>("InsDog"))
                .act(new Sink<>(System.out::println))
                .end();
  }
}
```

Here is a note about behaviors when you specify the execution order in `@AutotestExecution`, which is against the dependency declarations by `@DependsOn` and others.
They are not defined as of now.

**Execution Plan:**
```text
[INFO ] [2024/12/27 17:00:37.027] [main] - ----
[INFO ] [2024/12/27 17:00:37.028] [main] - Execution plan is as follows:
[INFO ] [2024/12/27 17:00:37.028] [main] - - beforeAll:      [setUpMethod]
[INFO ] [2024/12/27 17:00:37.028] [main] - - beforeEach:     []
[INFO ] [2024/12/27 17:00:37.028] [main] - - value:          [sceneMethod]
[INFO ] [2024/12/27 17:00:37.028] [main] - - afterEach:      []
[INFO ] [2024/12/27 17:00:37.028] [main] - - afterAll:       []
[INFO ] [2024/12/27 17:00:37.028] [main] - ----
```

**Action Tree: beforeAll:**

```text
[INFO ] [2024/12/27 17:00:37.035] [main] - LessonDependsOn     : beforeAll:  [o]setUpMethod                                                  
[INFO ] [2024/12/27 17:00:37.036] [main] - LessonDependsOn     : beforeAll:  +-[o:0]BEGIN[setUpMethod]@[work-id-1659515968]
[INFO ] [2024/12/27 17:00:37.036] [main] - LessonDependsOn     : beforeAll:  |-+-[o:0]let[InsDog][page]
[INFO ] [2024/12/27 17:00:37.036] [main] - LessonDependsOn     : beforeAll:  | +-[o:0]sink[page]
[INFO ] [2024/12/27 17:00:37.036] [main] - LessonDependsOn     : beforeAll:  +-[o:0]END[setUpMethod]
```

**Action Tree: test:**

```text
[INFO ] [2024/12/27 17:00:37.047] [main] - LessonDependsOn     : value:      [o]sceneMethod
[INFO ] [2024/12/27 17:00:37.048] [main] - LessonDependsOn     : value:      +-[o:0]BEGIN[sceneMethod]@[work-id-1337829755]
[INFO ] [2024/12/27 17:00:37.048] [main] - LessonDependsOn     : value:      |-+-[o:0]let[InsDog][page]
[INFO ] [2024/12/27 17:00:37.048] [main] - LessonDependsOn     : value:      | +-[o:0]sink[page]
[INFO ] [2024/12/27 17:00:37.048] [main] - LessonDependsOn     : value:      +-[o:0]END[sceneMethod]
```

Here is another question.
Except for a set-up method, what do we want to specify here?
You should specify other scene returning methods, without which the method don't make sense.
For instance, if you have a scene, that does something on a web page, but the page is a child of some others.
Without opening the child page, it doesn't make sense.
But, if we specify both of them, do we really want to specify it?
It's a valid question.
You can be lazy to skip it for now.
In longer term, the framework will implement other execution modes, such as "reverse order execution".
Until the day, it will not use the information.

## `@When`: Dependency for Assertions

`@When` is an annotation useful for defining "assertions".

<!-- @formatter:off -->
> 3.
>     2. For `@When`:
>         1. Implicit execution happens only when the scene mentioned by the `@When` is explicitly specified.
>         2. Implicit execution happens right after the scene mentioned by the `@When` annotation.
>
<!-- @formatter:off -->

Following code shows its usage.

```java

@AutotestExecution(defaultExecution = @Spec(
    value = "performTargetFunction",
    planExecutionWith = DEPENDENCY_BASED))
public class LessonWhen extends LessonBase {
  @Named
  public Scene performTargetFunction() {
    return Scene.begin().act("...").end();
  }
  
  @Named
  @When("performFunction")
  public Scene thenDatabaseRecordUpdated() {
    return Scene.begin().add(wasDatabaseRecordUpdated()).end();
  }
}
```

In the `@AutotestExecution` annotation, only `performFunction` is mentioned.
But the `thenDatabaseRecordUpdated` will be executed along with it.
In your IDE, it will be shown as:

```text
+ LessonWhen:
  + runTestAction:
    + [1]: performTargetFunction
    + [2]: thenDatabaseRecordWasUpdated 
```

If you focus on the method declaration is:

```java

@Named
@When("performFunction")
public Scene thenDatabaseRecordUpdated() {
  return Scene.begin().add(wasDatabaseRecordUpdated()).end();
}
```

This is as readable as "When performFunction, then database record was updated."

Not only that, perhaps, you may want to write multiple assertions for a single function.
That will look like in your IDE's test run window as follows.:

```text
+ LessonWhen:
  + runTestAction:
    + [1]: performTargetFunction
    + [2]: thenDatabaseRecordWasUpdated 
    + [3]: thenWindowWasUpdated 
```

Also, the execution plan and action tree look as follows.

**Execution Plan:**
```text
[INFO ] [2024/12/27 15:45:17.221] [main] - ----
[INFO ] [2024/12/27 15:45:17.221] [main] - Execution plan is as follows:
[INFO ] [2024/12/27 15:45:17.221] [main] - - beforeAll:      []
[INFO ] [2024/12/27 15:45:17.221] [main] - - beforeEach:     []
[INFO ] [2024/12/27 15:45:17.222] [main] - - value:          [performFunction, thenDatabaseRecordUpdated]
[INFO ] [2024/12/27 15:45:17.222] [main] - - afterEach:      []
[INFO ] [2024/12/27 15:45:17.222] [main] - - afterAll:       []
[INFO ] [2024/12/27 15:45:17.222] [main] - ----
```

**Action Tree**
```text
[INFO ] [2024/12/27 15:45:17.248] [main] - LessonWhen          : value:      [o]performFunction
[INFO ] [2024/12/27 15:45:17.248] [main] - LessonWhen          : value:      +-[o:0]BEGIN[performFunction]@[work-id-519303080]
[INFO ] [2024/12/27 15:45:17.248] [main] - LessonWhen          : value:      |-+-[o:0]let[Hello!][page]
[INFO ] [2024/12/27 15:45:17.248] [main] - LessonWhen          : value:      +-[o:0]END[performFunction]
[INFO ] [2024/12/27 15:45:17.256] [main] - LessonWhen          : value:      [o]thenDatabaseRecordUpdated
[INFO ] [2024/12/27 15:45:17.256] [main] - LessonWhen          : value:      +-[o:0]BEGIN[thenDatabaseRecordUpdated]@[work-id-1552400354]
[INFO ] [2024/12/27 15:45:17.257] [main] - LessonWhen          : value:      |-+-[o:0]let[Database record updated!][page]
[INFO ] [2024/12/27 15:45:17.257] [main] - LessonWhen          : value:      +-[o:0]END[thenDatabaseRecordUpdated]
```

When you want to run tests, you will declare the `performFunction` to be executed for sure.
But you may forget mentioning the assertion part.
This mechanism prevents it from happening.

What does this rule mean, then?

> Implicit execution happens only when the scene mentioned by the `@When` is explicitly specified.

Once `performTagetFunction` is defined and run as a test, we will need to use it as a part of a preparation (arranging)
step.
Because `targetFunction` will be reused in the product-side.

However, in such a situation, do we want to run `thenDatabaseRecordWasUpdate`?
No.

Here is what the rule means.
If `performTargetFunction` is mentioned in the `@AutotestExecution`, those `thenXyz` actions will be performed.
But if it is not, in other words, it is mentioned by other annotations such as `@DependsOn` and it is executed because
of it, `thenXyz` for it won't be performed.
Why does this make sense?
Because if `performTargetFunction` is reused in a preparation step, it will be mentioned by `@DependsOn` annotation
directly or indirectly.
Not directly in `@AutotestExecution`.
If it is directly mentioned in `@AutotestExecution`, the class is actually testing `performTargetFunction` and it will
be valid to run `thenXyz` whose `@When` specifies `performTargetFunction`.

## `@PreparedBy`: "Fallback" Dependency

<!-- @formatter: on -->
> 3.
>     3. For `@PreparedBy`:
>         1. Implicit execution happens only when it is the first `@PreparedBy` annotation for the scene or all the
>            previous ones failed.
>         2. After one sequence defined by `@PreparedBy` has succeeded the scene itself will be performed.
>            If the scene succeeds, the rest scenes provided by `@PreparedBy` will not be performed.
>         3. When the previous scene or any of `@PreparedBy` scene fails, the next one will be tried.
>            If it is the last one, the entire scene considered failed.
<!-- @formatter:on -->

When you write tests for a given system or a component, you will find that most of the test code consists of "
arrangement" part, which prepares preconditions to perform tests.
After some while, the next thing you will realize is "arrangement" part is very time-consuming and in some cases you
want to optimize it even if you are sacrificing "reliability" of tests.
That is, even if you know that it is necessary to re-install OS, to make tests 100% flakiness-free, reliable, and
repeatable, you cannot afford it, sometimes.
This is an extreme example, but this happens every day.
When you are testing Web-UI, after re-login and just moving to a homepage may cause differences in test results.
Still, we don't do log-out and log back in every time.

`@PreparedBy` is a mechanism to perform this sequence in a programmatic way.

```java

@AutotestExecution(defaultExecution = @Spec(
    value = "performScenario",
    planExecutionWith = DEPENDENCY_BASED))
public class LessonPreparedBy extends LessonBase {
  @Named
  public Scene login() {
    return Scene.begin().act("...").act("...").end();
  }
  
  @Named
  @PreparedBy({"toHomeScreen"})
  @PreparedBy({"loadLoginSession", "toHomeScreen"})
  @PreparedBy({"login", "saveLoginSession"})
  public Scene isLoggedIn() {
    return Scene.begin().act("...").act("...").end();
  }
  
  @Named
  @DependsOn("isLoggedIn")
  public Scene performScenario() {
    return Scene.begin().act("...").act("...").end();
  }
}  
```

This class models a test, where `performScenario` depends on `isLoggedIn`.
`isLoggedIn` succeeds when a test user is actually logged in.
But in modern web systems, it is becoming more and more expensive to log in them.
For instance, you may be required to do MFA, etc.
If it is a manual test, you would navigate to the home page of the system, then conduct the next test.
Even if you've closed a browser tab, still the cookie may remember the session, and you have a chance to go to the home
page without a problem.
Only when you run out of a way to keep conducting tests, you will do the log in again.

Following is an execution plan of the class.

```text
[INFO ] [2024/12/27 16:53:17.710] [main] - ----
[INFO ] [2024/12/27 16:53:17.710] [main] - Execution plan is as follows:
[INFO ] [2024/12/27 16:53:17.710] [main] - - beforeAll:      [isLoggedIn]
[INFO ] [2024/12/27 16:53:17.711] [main] - - beforeEach:     []
[INFO ] [2024/12/27 16:53:17.711] [main] - - value:          [performScenario]
[INFO ] [2024/12/27 16:53:17.711] [main] - - afterEach:      []
[INFO ] [2024/12/27 16:53:17.711] [main] - - afterAll:       []
[INFO ] [2024/12/27 16:53:17.711] [main] - ----
```

Only logged in is shown in the plan.
Action tree looks as follows (Edited for the conciseness sake).

```text
[INFO ] [2024/12/27 16:53:17.718] [main] - LessonPreparedBy    : beforeAll:  [o]isLoggedIn                                                   
[INFO ] [2024/12/27 16:53:17.718] [main] - LessonPreparedBy    : beforeAll:  [o:0]ensure:do sequentially using
[INFO ] [2024/12/27 16:53:17.718] [main] - LessonPreparedBy    : beforeAll:    |-+-[o:0]let[isLoggedIn][page]
[INFO ] [2024/12/27 16:53:17.719] [main] - LessonPreparedBy    : beforeAll:    | +-[o:0]sink[page]
[INFO ] [2024/12/27 16:53:17.719] [main] - LessonPreparedBy    : beforeAll:    +-[o:0]BEGIN[isLoggedIn]@[work-id-1636588948]
[INFO ] [2024/12/27 16:53:17.719] [main] - LessonPreparedBy    : beforeAll:    | |-+-[o:0]let[toHomeScreen][page]
[INFO ] [2024/12/27 16:53:17.719] [main] - LessonPreparedBy    : beforeAll:    +-[o:0]END[isLoggedIn]
[INFO ] [2024/12/27 16:53:17.719] [main] - LessonPreparedBy    : beforeAll:    +-[]BEGIN[isLoggedIn]@[work-id-662925691]
[INFO ] [2024/12/27 16:53:17.719] [main] - LessonPreparedBy    : beforeAll:    |-+-[]BEGIN[work-id-662925691]@[work-id-1977618945]
[INFO ] [2024/12/27 16:53:17.719] [main] - LessonPreparedBy    : beforeAll:    | |-+-[]let[loadLoginSession][page]
[INFO ] [2024/12/27 16:53:17.719] [main] - LessonPreparedBy    : beforeAll:    | +-[]END[work-id-662925691]
[INFO ] [2024/12/27 16:53:17.720] [main] - LessonPreparedBy    : beforeAll:    | +-[]BEGIN[work-id-662925691]@[work-id-1060519157]
[INFO ] [2024/12/27 16:53:17.720] [main] - LessonPreparedBy    : beforeAll:    | |-+-[]let[toHomeScreen][page]
[INFO ] [2024/12/27 16:53:17.720] [main] - LessonPreparedBy    : beforeAll:    | +-[]END[work-id-662925691]
[INFO ] [2024/12/27 16:53:17.720] [main] - LessonPreparedBy    : beforeAll:    | +-[]BEGIN[work-id-662925691]@[work-id-1060519157]
[INFO ] [2024/12/27 16:53:17.720] [main] - LessonPreparedBy    : beforeAll:    | |-+-[]let[login][page]
[INFO ] [2024/12/27 16:53:17.720] [main] - LessonPreparedBy    : beforeAll:    | | +-[]sink[page]
[INFO ] [2024/12/27 16:53:17.720] [main] - LessonPreparedBy    : beforeAll:    | |-+-[]let[saveLoginSession][page]
[INFO ] [2024/12/27 16:53:17.720] [main] - LessonPreparedBy    : beforeAll:    | +-[]END[work-id-662925691]
[INFO ] [2024/12/27 16:53:17.721] [main] - LessonPreparedBy    : beforeAll:    +-[]END[isLoggedIn]
```

Similar situation happens everywhere in testing.
Unless you know data sets in your system, you don't want to reload them, even if it is automated.
Ultimately, for the reliability's sake, it is necessary to be able to re-provision the entire system from the bare-metal
operating system automatically, in theory.
Yes, it is possible.
However, still reusing the state which was prepared by other tests is necessary at the same time.

`@PreparedBy` notation and the mechanism provides a uniformed way achieve this.

## `@ClosedBy`: Resource Clean-up

Sometimes a test consumes scarce system resources.
Such resources sometimes require clean-ups.
Database connection is just one example.

<!-- @formatter:off -->
> 3.
>    4. For `@ClosedBy`:
>        1. This is only valid for scenes in `beforeAll` and `beforeEach` stages.
>        2. If a scene annotated with this in `beforeAll` stage succeeds, the scene specified by this annotation will be
>           attempted in `afterAll` stage.
>        3. If it is in `beforeEach` stage, the specified scene will be attempted in `afterEach` stage.
<!-- @formatter:on -->

In the context of testing, this concern happens in arrangement step before actual tests.
In **InsDog**'s execution model, it means they happen `beforeAll` and `beforeEach` stages.
If a resource is allocated in `beforeAll`, it should be released in `afterAll`.
If it is `beforeEach`, it should be in `afterEach`.
If multiple resources are allocated, they should be released in the revered order.

Following is a code example of the usage of `@ClosedBy` annotation.:

```java

@AutotestExecution(defaultExecution = @Spec(
    value = "performScenario",
    planExecutionWith = DEPENDENCY_BASED))
public class LessonClosedBy extends LessonBase {
  @Named
  @ClosedBy("closeExecutionSession")
  public Scene openExecutionSession() {
    return Scene.begin().act("...").act("...").end();
  }
  
  @Named
  @DependsOn("openExecutionSession")
  public Scene closeExecutionSession() {
    return Scene.begin().act("...").act("...").end();
  }
  
  @Named
  @DependsOn("openExecutionSession")
  public Scene performScenario() {
    return Scene.begin().act("...").act("...").end();
  }
}
```

The main entry point `performScenario` depends on `openExecutionSession`.
Therefore, `openExecutionSession` will be executed in the `beforeAll` stage.
As `openExecutionSession` is annotated with `@ClosedBy("closeExecutionSession")`, the `closeExecutionSession` will be
performed.
Since `openExecutionSession` is performed in `beforeAll`, corresponding closing operation: `closeExecutionSession`, will
be performed in `afterAll`[^1].
Note that `closeExecutionSession` will be performed unless the `openExecutionSession` succeeds.
So, it is highly recommended to write the `openExecutionSession` in an "atomic" manner, where an operation completely
succeeds, otherwise it leaves no side effect at all.

**Execution Plan:**
```text
[INFO ] [2024/12/27 17:27:13.746] [main] - ----
[INFO ] [2024/12/27 17:27:13.746] [main] - Execution plan is as follows:
[INFO ] [2024/12/27 17:27:13.746] [main] - - beforeAll:      [openExecutionSession]
[INFO ] [2024/12/27 17:27:13.746] [main] - - beforeEach:     []
[INFO ] [2024/12/27 17:27:13.746] [main] - - value:          [performScenario]
[INFO ] [2024/12/27 17:27:13.747] [main] - - afterEach:      []
[INFO ] [2024/12/27 17:27:13.747] [main] - - afterAll:       []
[INFO ] [2024/12/27 17:27:13.747] [main] - ----
```

`closeExecutionSession` should be executed in the `afterAll` stage, but it is not shown.
The reason why is, because `closeExecutionSession` may not be executed in case `openExecutionSession` fails.
However, this is a matter of design choice and this behavior may be modified in the future.

**Action Tree (beforeAll)**

Since `openExecutionSession` is depended on by `performScenario`, it is automatically executed. 

```text
[INFO ] [2024/12/27 17:27:13.786] [main] - LessonClosedBy      : beforeAll:  [o]openExecutionSession                                         
[INFO ] [2024/12/27 17:27:13.786] [main] - LessonClosedBy      : beforeAll:  +-[o:0]BEGIN[openExecutionSession]@[work-id-1620459733]
[INFO ] [2024/12/27 17:27:13.786] [main] - LessonClosedBy      : beforeAll:  |-[o:0]let[openExecutionSession][page]
[INFO ] [2024/12/27 17:27:13.786] [main] - LessonClosedBy      : beforeAll:  +-[o:0]END[openExecutionSession]
```

**Action Tree (test)**

The main scenario: `performScenario`, which is explicitly specified in the execution directive, is performed. 

```text
[INFO ] [2024/12/27 17:27:13.811] [main] - LessonClosedBy      : value:      [o]performScenario
[INFO ] [2024/12/27 17:27:13.811] [main] - LessonClosedBy      : value:      +-[o:0]BEGIN[performScenario]@[work-id-1976166251]
[INFO ] [2024/12/27 17:27:13.811] [main] - LessonClosedBy      : value:      |-[o:0]let[openExecutionSession][page]
[INFO ] [2024/12/27 17:27:13.811] [main] - LessonClosedBy      : value:      +-[o:0]END[performScenario]
```

**Action Tree (afterAll)**

As we saw above, `openExecutionSession` is executed in `beforeAll` stage, and it was successfully finished.
`closeExecutionSession`, which is specified in `@ClosedBy` annotation of `openExecutionSession`, is executed in the `afterAll` stage. 

```text
[INFO ] [2024/12/27 17:27:13.820] [main] - LessonClosedBy      : afterAll:   [o]closeExecutionSession                                         
[INFO ] [2024/12/27 17:27:13.820] [main] - LessonClosedBy      : afterAll:   +-[o:0]BEGIN[closeExecutionSession]@[work-id-435914790]
[INFO ] [2024/12/27 17:27:13.820] [main] - LessonClosedBy      : afterAll:   |-[o:0]let[closeExecutionSession][page]
[INFO ] [2024/12/27 17:27:13.821] [main] - LessonClosedBy      : afterAll:   +-[o:0]END[closeExecutionSession]
```

## Footnotes

* [^1]: `closeExecutionSession` is declared to be depending on `openExecutionSession`.
  This is because it needs to resolve the variable that holds a resource to be released.
  This design might be changed so that `closeExecutionSession` doesn't require the explicit declaration of `@DependsOn("openExecutionSession")`.