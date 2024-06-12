package jp.co.moneyforward.autotest.framework.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface DependsOn {
  Parameter[] value() default {};
  @interface Parameter {
    String name();
    String sourceSceneName();
    String fieldNameInSourceScene();
  }
}
