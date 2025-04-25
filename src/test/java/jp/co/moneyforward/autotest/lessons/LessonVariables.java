package jp.co.moneyforward.autotest.lessons;

import jp.co.moneyforward.autotest.framework.action.Act;
import jp.co.moneyforward.autotest.framework.action.Scene;
import jp.co.moneyforward.autotest.framework.annotations.*;
import jp.co.moneyforward.autotest.framework.annotations.AutotestExecution.Spec;

import static com.github.valid8j.pcond.forms.Printables.function;
import static java.util.Objects.requireNonNull;
import static jp.co.moneyforward.autotest.framework.utils.InsdogUtils.let;
import static jp.co.moneyforward.autotest.framework.testengine.PlanningStrategy.DEPENDENCY_BASED;

@AutotestExecution(defaultExecution = @Spec(
    value = "performTargetFunction",
    planExecutionWith = DEPENDENCY_BASED))
public class LessonVariables extends LessonBase {
  @Named
  @Export("basePage") // The "basePage" variable will be available from scenes depending on this scene.
  public Scene openBasePage() {
    return Scene.begin() // If you don't specify a default variable name, "page" will be used.
                .add("basePage", let(new Page("newPage"))) // The value is assigned to "basePage"
                .end();
  }
  
  @Named
  @Export({"page", "childPage"})
  @Given("openBasePage")
  public Scene performTargetFunction() {
    return Scene.begin("page")
                .add("page", clickButton1(), "basePage")
                .add("childPage", openChildPage(), "page")
                .add(Scene.begin("childPage")
                          .act(screenshot()) // screenshot is executed using "childPage" variable for input and output
                          .end())
                .end();
  }
  
  @Named
  @Export() // If no variable is specified, all variables in this context will be exported.
  @When("performTargetFunction")
  public Scene thenClickButton2() {
    return Scene.begin()
                .add("page", clickButton2(), "page")
                .end();
  }
  
  @Named
  @Export()
  @When("performTargetFunction")
  public Scene thenClickButton3() {
    return Scene.begin("page")
                .add(clickButton3())
                .end();
  }
  
  @Named
  @Export()
  @When("performTargetFunction")
  public String thenClickButton4(@From("basePage") Object page) {
    System.err.println("Hello, " + page.toString() + " !!!");
    return page.toString();
  }
  
  private Act<Object, Object> screenshot() {
    return new Act.Func<>(function("screenshot", o -> "screenshot:[" + printVariableValue(o) + "]"));
  }
  
  private Act<Object, Object> clickButton1() {
    return new Act.Func<>(function("clickButton1", o -> "clickButton1:[" + printVariableValue(o) + "]"));
  }
  
  private Act<Object, Object> clickButton2() {
    return new Act.Func<>(function("clickButton2", o -> "clickButton2:[" + printVariableValue(o) + "]"));
  }
  
  private Act<Object, Object> clickButton3() {
    return new Act.Func<>(function("clickButton3", o -> "clickButton3:[" + printVariableValue(o) + "]"));
  }
  
  private Act<Object, Object> openChildPage() {
    return new Act.Func<>(function("openChildPage", o -> "openChildPage:[" + printVariableValue(o) + "]"));
  }
  
  private static Object printVariableValue(Object o) {
    System.out.println(o);
    return requireNonNull(o);
  }
  
  public static class Page {
    private final String name;
    
    Page(String name) {
      this.name = name;
    }
    
    public String toString() {
      return "page:[" + name + "]";
    }
    
  }
}
