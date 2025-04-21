package jp.co.moneyforward.autotest.framework.action;

import com.github.dakusui.actionunit.core.Action;
import com.github.dakusui.actionunit.core.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.valid8j.classic.Requires.requireNonNull;
import static jp.co.moneyforward.autotest.framework.internal.InternalUtils.action;
import static jp.co.moneyforward.autotest.framework.internal.InternalUtils.trivialAction;

/// A class to model a "call" to a `Scene`.
public final class SceneCall implements Call, WithOid {
  private final Scene scene;
  private final ResolverBundle resolverBundle;
  private final String outputVariableStoreName;
  
  /// Creates an instance of this class.
  ///
  /// outputVariableStoreName specifies a name of a context variable (**actionunit**), to which the output of `scene`
  /// is written.
  ///
  /// `resolverBundle` is used to compute input variable values.
  ///
  /// @param scene                   A scene to be performed by this call.
  /// @param resolverBundle          A bundle of resolvers.
  /// @param outputVariableStoreName A name of variable store, to which the `scene` writes its output.
  public SceneCall(Scene scene,
                   ResolverBundle resolverBundle,
                   String outputVariableStoreName) {
    this.outputVariableStoreName = requireNonNull(outputVariableStoreName);
    this.scene = requireNonNull(scene);
    this.resolverBundle = requireNonNull(resolverBundle);
  }
  
  @Override
  public Action toAction(ActionComposer actionComposer) {
    return actionComposer.create(this);
  }
  
  /// Returns an object identifier of this object.
  ///
  /// @return An object identifier of this object.
  @Override
  public String oid() {
    return this.targetScene().oid();
  }
  
  /// Returns a `Scene` object targeted by this call.
  ///
  /// @return A `Scene` object.
  public Scene targetScene() {
    return this.scene;
  }
  
  /// A name of "output" variable store, where this `SceneCall` writes its final result at the end of execution.
  ///
  /// @return A name of variable store, which this `SceneCall` writes its result to.
  public String outputVariableStoreName() {
    return this.outputVariableStoreName;
  }
  
  /// Returns a bundle of variable resolvers of this object.
  ///
  /// @return A bundle of variable resolvers.
  public ResolverBundle resolverBundle() {
    return resolverBundle;
  }
  
  /// Returns currently ongoing working variable store.
  ///
  /// @param context A context in which this call is being executed.
  /// @return A currently ongoing working variable store.
  public Map<String, Object> workingVariableStore(Context context) {
    return context.valueOf(this.workingVariableStoreName());
  }
  
  /// Returns an action, which marks the beginning of a sequence of main actions.
  ///
  /// The action copies a map of the InsDog's framework variables for this scene call to a context variable whose name
  /// is computed by `workingVariableStoreNameFor(this.targetScene().oid())`.
  ///
  /// @return An action, which marks the beginning of a sequence of main actions.
  public Action begin(List<String> ongoingWorkingVariableStoreNames) {
    try {
      String ongoingWorkingVariableStoreName = ongoingWorkingVariableStoreNames.isEmpty() ? null
                                                                                          : ongoingWorkingVariableStoreNames.getLast();
      return action("variables:" + this.resolverBundle().keySet(),
                    c -> c.assignTo(workingVariableStoreName(),
                                    composeWorkingVariableStore(ongoingWorkingVariableStoreName,
                                                                this,
                                                                c)));
    } finally {
      ongoingWorkingVariableStoreNames.add(workingVariableStoreName());
    }
  }
  
  /// Returns an action, which marks an ending of a sequence of main actions.
  ///
  /// The action copies to a map of the InsDog's framework variables for this scene call from a context variable whose
  /// name is computed by `workingVariableStoreNameFor(this.targetScene().oid())`.
  ///
  /// @return An action, which marks an ending of a sequence of main actions.
  public Action end(List<String> ongoingWorkingVariableStoreNames) {
    ongoingWorkingVariableStoreNames.removeLast();
    return trivialAction("END[" + outputVariableStoreName() + "]", c -> {
      c.assignTo(outputVariableStoreName(), c.valueOf(workingVariableStoreName()));
      c.unassign(workingVariableStoreName());
    });
  }
  
  //  Copies the map stored as "work area" to `outputFieldName` variable.
  
  /// Returns a map (variable store), with which a targetScene can interact to store/read data.
  /// Initial values of variables are resolved by giving a `context` parameter value to each element in `resolverBundle`.
  ///
  /// @param sceneCall A scene call for which a returned map is created.
  /// @param context   A context in which actions created from the target scene are performed.
  /// @return A data store map.
  /// @see ResolverBundle
  private static Map<String, Object> composeWorkingVariableStore(String ongoingWorkingVariableStoreName,
                                                                 SceneCall sceneCall,
                                                                 Context context) {
    Map<String, Object> ret = context.defined(ongoingWorkingVariableStoreName) ? context.valueOf(ongoingWorkingVariableStoreName)
                                                                               : new HashMap<>();
    sceneCall.resolverBundle()
             .forEach((k, r) -> ret.put(k, r.apply(context)));
    return ret;
  }
}
