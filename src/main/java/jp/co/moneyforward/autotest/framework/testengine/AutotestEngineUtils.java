package jp.co.moneyforward.autotest.framework.testengine;

import com.github.dakusui.actionunit.actions.cmd.Commander;
import jp.co.moneyforward.autotest.framework.action.*;
import jp.co.moneyforward.autotest.framework.annotations.From;
import jp.co.moneyforward.autotest.framework.annotations.PreparedBy;
import jp.co.moneyforward.autotest.framework.annotations.To;
import jp.co.moneyforward.autotest.framework.core.AutotestException;
import jp.co.moneyforward.autotest.framework.core.AutotestRunner;
import jp.co.moneyforward.autotest.framework.exceptions.MethodInvocationException;
import jp.co.moneyforward.autotest.framework.internal.InternalUtils;
import jp.co.moneyforward.autotest.framework.utils.InsdogUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;

import static java.util.stream.Collectors.joining;
import static jp.co.moneyforward.autotest.framework.action.ResolverBundle.resolverBundleFromDependenciesOf;
import static jp.co.moneyforward.autotest.framework.action.Scene.DEFAULT_DEFAULT_VARIABLE_NAME;
import static jp.co.moneyforward.autotest.framework.internal.InternalUtils.wrap;

public enum AutotestEngineUtils {
  ;
  
  public static <T> List<T> topologicalSort(List<T> specified, Map<T, List<T>> graph) {
    Set<T> visited = new LinkedHashSet<>();
    specified.forEach(each -> traverseDependencies(each, graph, visited));
    return new LinkedList<>(visited);
  }
  
  private static <T> void traverseDependencies(T node, Map<T, List<T>> graph, Set<T> visited) {
    if (visited.contains(node))
      return;
    if (!graph.containsKey(node)) {
      throw new NoSuchElementException("Unknown node:<" + node + "> was given. Known nodes are: " + graph.keySet());
    }
    graph.get(node).forEach(each -> traverseDependencies(each, graph, visited));
    visited.add(node);
  }
  
  static <T> List<T> mergeListsByAppendingMissedOnes(List<T> list1, List<T> list2) {
    List<T> ret = new ArrayList<>(list1);
    for (T item : list2) {
      if (!ret.contains(item)) {
        ret.add(item);
      }
    }
    return ret;
  }
  
  private static Scene createScene(PreparedBy preparedByValue, Class<?> accessModelClass, AutotestRunner runner) {
    // defaultVariableName of Scene.Builder is only used during build process.
    // Once a scene is built, it won't be used neither by the scene nor the builder.
    Scene.Builder b = new Scene.Builder();
    Arrays.stream(preparedByValue.value())
          .map(n -> InternalUtils.findMethodByName(n, accessModelClass).orElseThrow(NoSuchElementException::new))
          .map(m -> methodToScene(m, runner)).forEach(b::add);
    return b.name("ensurer").build();
  }
  
  private static SceneCall sceneToSceneCall(Scene scene, ResolverBundle resolverBundle, String outputVariableStoreName) {
    return AutotestSupport.sceneToSceneCall(scene, outputVariableStoreName, resolverBundle);
  }
  
  private static Scene methodToScene(Method method, AutotestRunner runner) {
    try {
      return switch (method) {
        case Method m when Scene.class.isAssignableFrom(m.getReturnType()) ->
            methodToSceneByArrangeTimeInvocation(m, runner);
        case Method m when !Scene.class.isAssignableFrom(m.getReturnType()) ->
            methodToSceneByActTimeInvocation(m, runner);
        default -> unsupportedMethod(method);
      };
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new MethodInvocationException("Failed to create a scene with: " + method, e);
    }
  }
  
  private static Scene methodToSceneByArrangeTimeInvocation(Method method, AutotestRunner runner) throws IllegalAccessException, InvocationTargetException {
    return (Scene) method.invoke(runner);
  }
  
  private static Scene unsupportedMethod(Method method) {
    throw new UnsupportedOperationException(describeMethod(method));
  }
  
  private static Scene methodToSceneByActTimeInvocation(Method method, AutotestRunner runner) {
    Method m = validateMethodToDefineSceneIndirectly(method);
    String inputVariableName = "*ALL*";
    String outputVariableName = Optional.ofNullable(m.getAnnotation(To.class))
                                        .map(To::value)
                                        .orElse(Scene.DUMMY_OUTPUT_VARIABLE_NAME);
    return Scene.begin()
                .add(outputVariableName,
                     methodToAct(runner, m),
                     inputVariableName)
                .end();
  }
  
  private static Act<?, ?> methodToAct(AutotestRunner runner, Method method) {
    return InsdogUtils.func((Object in) -> {
      try {
        @SuppressWarnings("unchecked") Map<String, Object> vars = (Map<String, Object>) in;
        return method.invoke(runner, composeArgsFor(method, vars));
      } catch (RuntimeException e) {
        throw new AutotestException(MessageFormat.format("Failed to execute: {0}: {1}",
                                                         composeDescriptionFor(method, runner, in),
                                                         e.getMessage()),
                                    e);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw wrap(e);
      }
    }).describe(method.getName());
  }
  
  private static Object[] composeArgsFor(Method method, Map<String, Object> in) {
    List<String> errors = new ArrayList<>();
      Object[] ret = Arrays.stream(method.getParameters())
                             .map(p -> p.isAnnotationPresent(From.class) ? p.getAnnotation(From.class).value()
                                                                         : DEFAULT_DEFAULT_VARIABLE_NAME)
                             .peek(from -> {
                               if (!in.containsKey(from))
                                 errors.add(from);
                             })
                             .map(in::get)
                             .toArray();
      if (!errors.isEmpty())
        throw new AutotestException("Undefined variables: " + errors, null);
      return ret;
  }
  
  private static String composeDescriptionFor(Method m, AutotestRunner runner, Object in) {
    return MessageFormat.format("Failed to invoke: {0} on: <{1}> with: <{2}>", describeMethod(m), runner, in);
  }
  
  private static String describeMethod(Method m) {
    return MessageFormat.format("{0}#{1}{2}",
                                m.getDeclaringClass().getCanonicalName(),
                                m.getName(),
                                Arrays.stream(m.getParameterTypes())
                                      .map(Class::getSimpleName)
                                      .collect(joining(",", "(", ")")));
  }
  
  static Call methodToCall(Method method, Class<?> accessModelClass, AutotestRunner runner) {
    PreparedBy[] preparedByAnnotations = method.getAnnotationsByType(PreparedBy.class);
    if (preparedByAnnotations.length > 0) {
      return new EnsuredCall(sceneToSceneCall(methodToScene(method, runner),
                                              resolverBundleFromDependenciesOf(method, accessModelClass),
                                              InternalUtils.nameOf(method)),
                             annotationsToEnsurers(preparedByAnnotations, accessModelClass, runner, method),
                             resolverBundleFromDependenciesOf(method, accessModelClass));
    }
    return sceneToSceneCall(methodToScene(method, runner),
                            resolverBundleFromDependenciesOf(method, accessModelClass),
                            InternalUtils.nameOf(method));
  }
  
  private static List<SceneCall> annotationsToEnsurers(PreparedBy[] preparedByAnnotations, Class<?> accessModelClass, AutotestRunner runner, Method targetMethod) {
    return Arrays.stream(preparedByAnnotations).map(ann -> new SceneCall(createScene(ann, accessModelClass, runner), resolverBundleFromDependenciesOf(targetMethod, accessModelClass), InternalUtils.nameOf(targetMethod))).toList();
  }
  
  private static Method validateMethodToDefineSceneIndirectly(Method method) {
    return method;
  }
}
