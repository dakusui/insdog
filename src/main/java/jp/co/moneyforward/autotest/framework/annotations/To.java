package jp.co.moneyforward.autotest.framework.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

///  An annotation that indicates a name of a variable to which the value returned by the method is assigned.
///  This annotation is attached to a method that returns a non-`Scene` value.
@Retention(RUNTIME)
@Target(METHOD)
public @interface To {
  /// @return A name of the variable to which the value returned by the annotated method is assigned.
  String value();
}
