package jp.co.moneyforward.autotest.framework.action;

import com.github.dakusui.actionunit.core.Action;
import com.github.dakusui.actionunit.core.Context;
import jp.co.moneyforward.autotest.framework.utils.InternalUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.github.valid8j.classic.Requires.requireNonNull;

/**
 * A class to model a "call" to a `Scene`.
 */
public final class SceneCall implements Call {
  final Scene scene;
  final Map<String, Function<Context, Object>> assignmentResolvers;
  private final String outputStoreName;
  
  public SceneCall(String outputStoreName, Scene scene, Map<String, Function<Context, Object>> assignmentResolvers) {
    this.outputStoreName = requireNonNull(outputStoreName);
    this.scene = requireNonNull(scene);
    this.assignmentResolvers = requireNonNull(assignmentResolvers);
  }
  
  public SceneCall(Scene scene) {
    this.outputStoreName = null;
    this.scene = requireNonNull(scene);
    this.assignmentResolvers = null;
  }
  
  public Scene targetScene() {
    return this.scene;
  }
  
  public String workingVariableStoreName() {
    return "work-" + objectId();
  }
  
  
  public Optional<Map<String, Function<Context, Object>>> assignmentResolvers() {
    return Optional.ofNullable(assignmentResolvers);
  }
  
  Map<String, Object> workingVariableStore(Context context) {
    return context.valueOf(workingVariableStoreName());
  }
  
  @Override
  public String outputVariableName() {
    return requireNonNull(this.outputStoreName);
  }
  
  public String outputStoreName() {
    return requireNonNull(this.outputStoreName);
  }
  
  @Override
  public List<String> inputVariableNames() {
    return scene.children()
                .stream()
                .flatMap(c -> c.inputVariableNames()
                               .stream())
                .distinct()
                .toList();
  }
  
  @Override
  public Action toAction(ActionComposer actionComposer, Map<String, Function<Context, Object>> assignmentResolversFromCurrentCall) {
    return actionComposer.create(this, assignmentResolversFromCurrentCall);
  }
  
  public Action begin(Map<String, Function<Context, Object>> assignmentResolversFromCurrentCall) {
    return beginSceneCall(this, assignmentResolversFromCurrentCall);
  }
  
  public Action end() {
    if (this.outputStoreName != null)
      return endSceneCall(this);
    return endSceneCallDismissingOutput(this);
  }
  
  private static Map<String, Object> initializeWorkingVariableStore(SceneCall sceneCall,
                                                                    Context context,
                                                                    Map<String, Function<Context, Object>> assignmentResolversFromCurrentCall) {
    var ret = new HashMap<String, Object>();
    sceneCall.assignmentResolvers().orElse(assignmentResolversFromCurrentCall)
             .forEach((k, r) -> ret.put(k, r.apply(context)));
    return ret;
  }
  
  
  private static Action beginSceneCall(SceneCall sceneCall, Map<String, Function<Context, Object>> assignmentResolversFromCurrentCall) {
    return InternalUtils.action("BEGIN@" + sceneCall.scene.name(),
                                c -> c.assignTo(sceneCall.workingVariableStoreName(),
                                                initializeWorkingVariableStore(sceneCall,
                                                                               c,
                                                                               assignmentResolversFromCurrentCall)));
  }
  
  /*
   * Copies the map stored as "work area" to `outputFieldName` variable.
   */
  private static Action endSceneCall(SceneCall sceneCall) {
    return InternalUtils.action("END@" + sceneCall.scene.name(), c -> {
      c.assignTo(sceneCall.outputStoreName(), c.valueOf(sceneCall.workingVariableStoreName()));
      c.unassign(sceneCall.workingVariableStoreName());
    });
  }
  
  private static Action endSceneCallDismissingOutput(SceneCall sceneCall) {
    return InternalUtils.action("END[dismissingOutput]@" + sceneCall.scene.name(), c -> {
    });
  }
  
  
  private String objectId() {
    return scene.name() + ":" + System.identityHashCode(scene);
  }
}
