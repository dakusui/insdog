package jp.co.moneyforward.autotest.framework.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation is attached to an action method (a `Scene` returning method).
 *
 * Even if the method to which this annotation is attached is not included in the explicit test scenario, it will be
 * executed as long as its target method (specified by `value()` attribute of this annotation) is a part of the main test scenario.
 *
 * The scene created by the attached method will be executed right after the target method.
 *
 * if there are following methods:
 *
 * ```java
 * &#64;Named
 * &#64;DependsOn(&#64;Parameter(name="page", source="login"))
 * Scene targetMethod() {
 *   return scene;
 * }
 *
 * &#64;Named
 * &#64;When("targetMethod")
 * Scene checkMethod() {
 *    return scene;
 * }
 * ```
 *
 * And in case only `targetMethod` is specified as a part of the main test scenario, the `checkMethod` will still be executed
 * right after `targetMethod`.
 * The same dependency declaration as the `targetMethod` will be used for the `checkMethod`, too.
 *
 * Even if the target method is executed because it is depended on by others during the **beforeAll** step, the `&#64;When` annotated method won't be executed.
 * This behavior is useful when a programmer wants to focus on a specific part of a test and save time.
 *
 * @see DependsOn
 */
@Retention(RUNTIME)
public @interface When {
  /**
   * Specifies a scene name, for which the scene method references.
   *
   * @return A name of a scene.
   */
  String value();
}
