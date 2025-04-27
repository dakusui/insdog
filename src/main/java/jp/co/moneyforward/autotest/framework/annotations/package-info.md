This package hols annotations defined by the **InsDog** framework.

## `@AutotestExecution`: Test Class's Fallback Behaviors

This annotation specifies the fallback (default) behaviors of your test class.
The behavior can be overridden through CLI parameters.

**NOTE:** Currently, there is no way to override the default behavior from higher level.
You need to modify the code directly and re-compile it for now.

## `@Named`: Identity

This annotation specifies a name of an entity, typically a **Scene** providing method.
In case, you don't give a `value` of it, the framework considers the name of the entity, i.e. a name of the method if it is a method, is the name of it.

Note that only with the name, the **InsDog** framework-core identifies an entity.
Meaning that if you don't give this annotation, it simply doesn't recognize a method you create at all.

**NOTE:** Currently, the framework doesn't do validations on your class at all.
Be careful.

## `@Export`, `@To`: Exporting Variables for Other Scenes

This annotation can be attached to a **Scene** providing method.
Its value specifies the variable names that can be used by other **Scene** providing methods, which depend on the attached **Scene** providing method.
`@To` is very similar to `@Export`.
The difference is you can only specify one variable with `@To`.
For non-`Scene` returning methods, `@To` should be preferred over `@Export`.

## `@Given`(`@DependsOn`), `@When`, and `@ClosedBy`

If you choose a `PlanningSteatedy.DEPENDENCY_BASED` for `@AutotextExecution.Spec#planExecutionWith`, the **InsDog** framework respects the annotations `@Given`, `@When`, and `@ClosedBy` attached to the **Scene** providing methods.

`@DependsOn` is a synonym for `@Given`, which is already deprecated and planned to retire.

For more details of their semantics, check respective documents of annotations and the `PlanningStrategy`.

## Usages

* `@AutotestExecution`
* `@ClosedBy`

## References

@see jp.co.moneyforward.autotest.framework.testengine.PlanningStrategy

