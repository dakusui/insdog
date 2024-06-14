package jp.co.moneyforward.autotest.ut.framework.execution.context_variables;

import com.github.dakusui.actionunit.core.Action;
import com.github.dakusui.actionunit.core.Context;
import com.github.dakusui.actionunit.io.Writer;
import com.github.dakusui.actionunit.visitors.ReportingActionPerformer;
import com.github.valid8j.fluent.Expectations;
import jp.co.moneyforward.autotest.framework.action.Call;
import jp.co.moneyforward.autotest.framework.action.Scene;
import jp.co.moneyforward.autotest.framework.facade.AutotestSupport;
import jp.co.moneyforward.autotest.framework.core.Resolver;
import jp.co.moneyforward.autotest.ututils.ActUtils;
import jp.co.moneyforward.autotest.ututils.TestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.github.valid8j.fluent.Expectations.assertStatement;
import static com.github.valid8j.fluent.Expectations.value;
import static jp.co.moneyforward.autotest.framework.facade.AutotestSupport.*;
import static jp.co.moneyforward.autotest.ututils.ActUtils.*;
import static jp.co.moneyforward.autotest.ututils.ActionUtils.*;

public class VariablesTest extends TestBase {
  @Test
  public void givenSceneWithVariableReadingAct_whenToActionExecuted_thenActionTreeLooksCorrect() {
    LinkedList<String> out = new LinkedList<>();
    Scene scene = scene(List.of(leafCall("x", ActUtils.let("Scott Tiger")),
                                leafCall("x", helloAct(), "x"),
                                leafCall("x", printlnAct(), "x"),
                                leafCall("x", addToListAct(out), "x")));
    
    
    Action action = sceneCall("output", scene, List.of()).toAction(createActionComposer());
    
    performAction(action, Writer.Std.OUT);
    
    Expectations.assertStatement(Expectations.value(out).elementAt(0).asString().toBe().containing("HELLO").containing("Scott Tiger"));
  }
  
  @Test
  public void takeOvers() {
    LinkedList<String> out = new LinkedList<>();
    Scene scene = scene(List.of(
        sceneCall("SCENE1",
                  List.of(leafCall("out", let("Scott Tiger")),
                          leafCall("x", helloAct(), "out")),
                  List.of()),
        sceneCall("SCENE2",
                  List.of(leafCall("y", addToListAct(out), "in")),
                  List.of(new Resolver("in", valueFrom("SCENE1", "x"))))));
    
    
    Action action = sceneCall("output", scene, List.of()).toAction(createActionComposer());
    
    ReportingActionPerformer actionPerformer = createReportingActionPerformer();
    actionPerformer.performAndReport(action, Writer.Std.OUT);
    
    assertStatement(value(out).elementAt(0)
                              .asString()
                              .toBe()
                              .containing("HELLO")
                              .containing("Scott Tiger"));
  }
  
  @Test
  public void takeOvers2() {
    Scene scene = scene(List.of(
        sceneCall("SCENE1",
                  List.of(leafCall("out", let("Scott Tiger")),
                          leafCall("x", helloAct(), "out")),
                  List.of()),
        sceneCall("SCENE2",
                  List.of(AutotestSupport.assertionCall("y", helloAct(), x -> value(x).toBe()
                                                                                      .startingWith("HELLO:")
                                                                                      .containing("Scott"), "in")),
                  List.of(new Resolver("in", valueFrom("SCENE1", "x"))))));
    
    Action action = sceneCall("OUT", scene, List.of()).toAction(createActionComposer());
    performAction(action, Writer.Std.OUT);
  }
  
  @Test
  public void action1() {
    Call.SceneCall sceneCall = sceneCall("sceneOut",
                                         List.of(leafCall("var", let("Scott"), "NONE"),
                                                 leafCall("var", helloAct(), "var"),
                                                 leafCall("var", printlnAct(), "var")),
                                         List.of());
    performAction(createActionComposer().create(sceneCall), Writer.Std.OUT);
  }
  
  
  @Test
  public void action2() {
    Call.SceneCall sceneCall1 = sceneCall("S1",
                                          List.of(
                                              leafCall("var", let("Scott")),
                                              leafCall("var", helloAct(), "var"),
                                              leafCall("var", printlnAct(), "var")),
                                          List.of());
    Call.SceneCall sceneCall2 = sceneCall("S2",
                                          List.of(leafCall("var", helloAct(), "foo"),
                                                  leafCall("var", printlnAct(), "foo")),
                                          List.of(new Resolver("foo", valueFrom("S1", "var"))));
    
    ReportingActionPerformer actionPerformer = createReportingActionPerformer();
    performAction(createActionComposer().create(sceneCall1), actionPerformer, Writer.Std.OUT);
    performAction(createActionComposer().create(sceneCall2), actionPerformer, Writer.Std.OUT);
  }
  
  @Test
  public void action3() {
    Call.SceneCall sceneCall1 = new Call.SceneCall("S1",
                                                   new Scene.Builder().addCall(leafCall("var", let("Scott"), "NONE"))
                                                                      .addCall(leafCall("var", helloAct(), "var"))
                                                                      .addCall(leafCall("var", printlnAct(), "var"))
                                                                      .build(), new HashMap<>());
    Call.SceneCall sceneCall2 = new Call.SceneCall("S2",
                                                   new Scene.Builder().addCall(leafCall("foo", helloAct(), "foo"))
                                                                      .addCall(getStringStringAssertionActCall())
                                                                      .build(),
                                                   new HashMap<>() {{
                                                     this.put("foo", new Function<Context, Object>() {
                                                       @Override
                                                       public Object apply(Context context) {
                                                         return context.<Map<String, Object>>valueOf("S1").get("var");
                                                       }
                                                     });
                                                   }});
    
    ReportingActionPerformer actionPerformer = createReportingActionPerformer();
    performAction(createActionComposer().create(sceneCall1), actionPerformer, Writer.Std.OUT);
    performAction(createActionComposer().create(sceneCall2), actionPerformer, Writer.Std.OUT);
  }
  
  private static Call.AssertionActCall<String, String> getStringStringAssertionActCall() {
    return new Call.AssertionActCall<>(new Call.LeafActCall<>("foo", printlnAct(), "foo"), s -> Expectations.value(s)
                                                                                                            .toBe()
                                                                                                            .containing("HELLO")
                                                                                                            .containing("Scott"));
  }
  
}
