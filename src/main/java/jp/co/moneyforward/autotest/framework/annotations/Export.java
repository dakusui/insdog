package jp.co.moneyforward.autotest.framework.annotations;

import jp.co.moneyforward.autotest.framework.action.Scene;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

///
/// // @formatter:off
/// An annotation to specify the fields that can be used by other scenes.
///
/// ```java
/// @Named
/// @Export("page")
/// public Scene aMethod() {
///   return Scene.begin("page")
///               .add(...)
///               .end();
/// }
///
/// @Named
/// @DependsOn("aMethod")
/// public Scene bMethod() {
///   return Scene.begin("object")
///               .add("object", func(v -> w), "page")
///               .end();
/// }
/// ```
///
/// The variables in the method with this annotation can be referenced by other method (in this case `bMethod`)
/// using `@DependsOn` annotation.
/// The scene in the referencing method can access variables specified in `@Export` annotation.
/// By default, all variables in the referenced-side are considered specified.
/// If an `@Export` is missing, default behavior will be performed.
/// As a result, all variables are exported by default even if you don't use this annotation.
///
/// NOTE: In longer term, this annotation might be renamed because its role is "to limit" variables to be exported
/// in practice, not "to export".
///
/// // @formatter:on
///
@Retention(RUNTIME)
public @interface Export {
  ///
  /// Variable names to be exported from the scene returned by the method this annotation is attached to.
  ///
  /// @return Exported variable names.
  ///
  String[] value() default {Scene.DEFAULT_DEFAULT_VARIABLE_NAME};
}
