package jp.co.moneyforward.autotest.framework.testengine;

import jp.co.moneyforward.autotest.framework.action.*;
import jp.co.moneyforward.autotest.framework.annotations.From;
import jp.co.moneyforward.autotest.framework.annotations.PreparedBy;
import jp.co.moneyforward.autotest.framework.annotations.To;
import jp.co.moneyforward.autotest.framework.core.AutotestRunner;
import jp.co.moneyforward.autotest.framework.exceptions.MethodInvocationException;
import jp.co.moneyforward.autotest.framework.internal.InternalUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static com.github.valid8j.fluent.Expectations.require;
import static com.github.valid8j.fluent.Expectations.value;
import static jp.co.moneyforward.autotest.framework.action.ResolverBundle.resolverBundleFromDependenciesOf;
import static jp.co.moneyforward.autotest.framework.action.Scene.DEFAULT_DEFAULT_VARIABLE_NAME;

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
        case Method m when Scene.class.isAssignableFrom(m.getReturnType()) && m.getParameterCount() == 0 ->
            methodToSceneByDirectInvocation(m, runner);
        case Method m when !m.getReturnType().equals(void.class) && m.getParameterCount() == 1 ->
            methodToSceneByIndirectInvocation(m, runner);
        default -> unsupportedMethod();
      };
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new MethodInvocationException("Failed to create a scene with: " + method, e);
    }
  }
  
  private static Scene methodToSceneByDirectInvocation(Method method, AutotestRunner runner) throws IllegalAccessException, InvocationTargetException {
    return (Scene) method.invoke(runner);
  }
  
  private static Scene unsupportedMethod() {
    throw new UnsupportedOperationException();
  }
  
  private static Scene methodToSceneByIndirectInvocation(Method method, AutotestRunner runner) {
    Method m = validateMethodToDefineSceneIndirectly(method);
    String inputVariableName = Optional.ofNullable(m.getParameterTypes()[0].getAnnotation(From.class))
                                       .map(From::value)
                                       .orElse(DEFAULT_DEFAULT_VARIABLE_NAME);
    String outputVariableName = Optional.ofNullable(m.getAnnotation(To.class))
                                        .map(To::value)
                                        .orElse(inputVariableName);
    return Scene.begin()
                .add(outputVariableName,
                     new Act.Func<>(m.getName(),
                                    (Object in) -> {
                                      try {
                                        return m.invoke(runner, in);
                                      } catch (IllegalAccessException | InvocationTargetException e) {
                                        throw InternalUtils.wrap(e);
                                      }
                                    }),
                     inputVariableName)
                .end();
  }
  
  static Call methodToCall(Method method, Class<?> accessModelClass, AutotestRunner runner) {
    PreparedBy[] preparedByAnnotations = method.getAnnotationsByType(PreparedBy.class);
    if (preparedByAnnotations.length > 0) {
      return new EnsuredCall(sceneToSceneCall(methodToScene(method, runner),
                                              resolverBundleFromDependenciesOf(method, accessModelClass),
                                              InternalUtils.nameOf(method)),
                             annotationsToEnsurers(preparedByAnnotations,
                                                   accessModelClass, runner, method),
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
