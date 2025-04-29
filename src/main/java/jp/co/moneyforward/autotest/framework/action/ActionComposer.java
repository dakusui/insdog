package jp.co.moneyforward.autotest.framework.action;

import com.github.dakusui.actionunit.actions.Ensured;
import com.github.dakusui.actionunit.core.Action;
import com.github.dakusui.actionunit.core.Context;
import com.github.valid8j.pcond.forms.Printables;
import jp.co.moneyforward.autotest.framework.core.ExecutionEnvironment;
import jp.co.moneyforward.autotest.framework.internal.InternalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.dakusui.actionunit.core.ActionSupport.*;
import static com.github.valid8j.fluent.Expectations.require;
import static com.github.valid8j.fluent.Expectations.value;
import static java.util.Collections.unmodifiableList;
import static jp.co.moneyforward.autotest.framework.internal.InternalUtils.*;

/// An interface that models a factory of actions.
///
/// This interface is designed to be a "visitor" of "calls", each of which represents a call of an action (`ActionFactory`).
///
/// A call is a model of an occurrence of an action, and it has input and output.
///
/// Calls can be categorized into two.
/// Calls for scenes (`Scene`) and calls for acts (`Act`).
/// Corresponding to the subclasses of `Act`, there are subcategories of it, which are `LeafAct`, `AssertionAct`, and `PipelinedAct`.
///
/// In this interface, there are `create(XyzCall xyzCall, Map<String, Function<Context, Object>> assignmentResolversFromCurrentCall)` methods defined.
///
/// `assignmentResolversFromCurrentCall` is a map from a variable name to a function which resolves its value from the
/// ongoing context object.
/// By relying on this object for resolving variable values referenced inside `Act` objects (, which are held by `Calls`), we can define `Act` objects
/// work in different variable spaces without changing code (transparent to variable space name, which is determined by a call's object name).
///
/// @see Call
/// @see Scene
/// @see Act
public interface ActionComposer {
  /// A logger object
  Logger LOGGER = LoggerFactory.getLogger(ActionComposer.class);
  
  /// Returns currently ongoing `SceneCall` object.
  ///
  /// @return Currently ongoing `SceneCall` object.
  SceneCall ongoingSceneCall();
  
  /// Returns all currently ongoing `SceneCall` objects.
  /// The last one is the most inner one.
  ///
  /// @return A list of currently ongoing `SceneCall` objects.
  List<SceneCall> ongoingSceneCalls();
  
  /// Returns an execution environment in which actions created by this composer objects are performed.
  ///
  /// @return An execution environment.
  ExecutionEnvironment executionEnvironment();
  
  List<String> ongoingWorkingVariableStoreNames();
  
  /// Creates an action for a given `SceneCall` object.
  ///
  /// @param sceneCall A scene call from which an action should be created.
  /// @return A sequential action created from `sceneCall`.
  default Action create(SceneCall sceneCall) {
    return sequential(concat(Stream.of(sceneCall.begin(this)),
                             flattenSequentialAction(sceneCall.targetScene()
                                                              .toSequentialAction(this)),
                             Stream.of(sceneCall.end(ongoingWorkingVariableStoreNames())))
                          .toList());
  }
  
  default Action create(EnsuredCall ensuredCall) {
    Ensured.Builder b = ensure(ensuredCall.targetCall().toAction(this));
    for (Call each : ensuredCall.ensurers()) {
      b.with(each.toAction(this));
    }
    return b.$();
  }
  
  default Action create(RetryCall retryCall) {
    return retry(retryCall.targetCall().toAction(this))
        .times(retryCall.times())
        .on(retryCall.onException())
        .withIntervalOf(retryCall.interval(), retryCall.intervalUnit())
        .$();
  }
  
  default Action create(AssertionCall<?> call) {
    return sequential(
        Stream.concat(
                  Stream.of(call.targetCall().toAction(this)),
                  call.assertionsAsActCalls()
                      .stream()
                      .map(each -> each.toAction(this)))
              .toList());
  }
  
  default Action create(ActCall<?, ?> actCall) {
    SceneCall ongoingSceneCall = ongoingSceneCall();
    String indentation = InternalUtils.spaces(ongoingWorkingVariableStoreNames().size() * 2);
    return InternalUtils.action(indentation + variableNameToString(actCall.outputVariableName()) + ":=" + actCall.act().name() + "[" + variableNameToString(actCall.inputVariableName()) + "]",
                                toContextConsumerFromAct(ongoingSceneCall,
                                                         actCall,
                                                         this.executionEnvironment()));
  }
  
  private static <T, R> Consumer<Context> toContextConsumerFromAct(SceneCall ongoingSceneCall,
                                                                   ActCall<T, R> actCall,
                                                                   ExecutionEnvironment executionEnvironment) {
    return toContextConsumerFromAct(Printables.function(actCall.inputVariableName(), c -> actCall.resolveVariable(ongoingSceneCall, c)),
                                    actCall.act(),
                                    actCall.outputVariableName(),
                                    ongoingSceneCall,
                                    executionEnvironment);
  }
  
  private static <T, R> Consumer<Context> toContextConsumerFromAct(Function<Context, T> inputVariableResolver,
                                                                   Act<T, R> act,
                                                                   String outputVariableName,
                                                                   SceneCall ongoingSceneCall,
                                                                   ExecutionEnvironment executionEnvironment) {
    return c -> {
      String targetSceneName = ongoingSceneCall.targetScene().name();
      String actName = act.name();
      LOGGER.debug("ENTERING: {}:{}", targetSceneName, actName);
      Map<String, Object> workingVariableStore = ongoingSceneCall.workingVariableStore(c);
      try {
        var v = act.perform(inputVariableResolver.apply(c), executionEnvironment);
        workingVariableStore.put(outputVariableName, v);
      } catch (Error | RuntimeException e) {
        String message = (MessageFormat.format("Scene<{0}>.Act<{1}>[{2}]: available variables are: {3}",
                                               ongoingSceneCall.outputVariableStoreName(),
                                               act.name(),
                                               inputVariableResolver.toString(),
                                               workingVariableStore.keySet()
                                                                   .stream()
                                                                   .sorted()
                                                                   .map(k -> MessageFormat.format("{0}={1}",
                                                                                                  k,
                                                                                                  workingVariableStore.get(k)))
                                                                   .toList()));
        
        LOGGER.error(e.getMessage());
        throw wrap(e);
      } finally {
        LOGGER.debug("LEAVING:  {}:{}", targetSceneName, actName);
      }
    };
  }
  
  static ActionComposer createActionComposer(final ExecutionEnvironment executionEnvironment) {
    return new ActionComposer() {
      final List<String> ongoingWorkingVariableStoreNames = new ArrayList<>();
      
      final List<SceneCall> ongoingSceneCalls = new LinkedList<>();
      
      @Override
      public SceneCall ongoingSceneCall() {
        return require(value(ongoingSceneCalls).toBe().notEmpty()).getLast();
      }
      
      @Override
      public List<SceneCall> ongoingSceneCalls() {
        return unmodifiableList(this.ongoingSceneCalls);
      }
      
      @Override
      public ExecutionEnvironment executionEnvironment() {
        return executionEnvironment;
      }
      
      @Override
      public Action create(SceneCall sceneCall) {
        try {
          this.ongoingSceneCalls.add(sceneCall);
          return ActionComposer.super.create(sceneCall);
        } finally {
          this.ongoingSceneCalls.removeLast();
        }
      }
      
      @Override
      public List<String> ongoingWorkingVariableStoreNames() {
        return ongoingWorkingVariableStoreNames;
      }
    };
  }
}
