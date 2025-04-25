package jp.co.moneyforward.autotest.framework.testengine;

import com.github.dakusui.actionunit.core.Action;
import com.github.dakusui.actionunit.io.Writer;
import com.github.valid8j.pcond.fluent.Statement;
import jp.co.moneyforward.autotest.framework.action.ActionComposer;
import jp.co.moneyforward.autotest.framework.action.Call;
import jp.co.moneyforward.autotest.framework.action.SceneCall;
import jp.co.moneyforward.autotest.framework.annotations.AutotestExecution;
import jp.co.moneyforward.autotest.framework.annotations.ClosedBy;
import jp.co.moneyforward.autotest.framework.annotations.Named;
import jp.co.moneyforward.autotest.framework.annotations.When;
import jp.co.moneyforward.autotest.framework.core.AutotestRunner;
import jp.co.moneyforward.autotest.framework.core.ExecutionEnvironment;
import jp.co.moneyforward.autotest.framework.internal.InternalUtils;
import jp.co.moneyforward.autotest.framework.utils.Valid8JCliches.MakePrintable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.dakusui.actionunit.core.ActionSupport.named;
import static com.github.dakusui.actionunit.core.ActionSupport.sequential;
import static com.github.dakusui.actionunit.exceptions.ActionException.wrap;
import static com.github.valid8j.classic.Requires.requireNonNull;
import static com.github.valid8j.fluent.Expectations.*;
import static com.github.valid8j.pcond.internals.InternalUtils.wrapIfNecessary;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toMap;
import static jp.co.moneyforward.autotest.framework.action.ActionComposer.createActionComposer;
import static jp.co.moneyforward.autotest.framework.core.ExecutionEnvironment.testResultDirectory;
import static jp.co.moneyforward.autotest.framework.internal.InternalUtils.*;
import static jp.co.moneyforward.autotest.framework.testengine.AutotestEngine.Stage.*;

/// The test execution engine of the **insdog**.
///
/// In the implementation of this engine, the steps performed during a test class execution are following:
///
/// 1. **beforeAll:** Every scene in this step is executed in the order they are shown in the execution plan.
/// 2. **beforeEach:** For each scene in the **value (main)** step, every scene in this step is executed in the order.
/// When a failure occurs, the rest will not be executed.
/// 3. **value (or main):** This step is the main part of the entire test.
/// This stage was named **value** to make the user test scenario class as simple as possible.
/// (In Java, in order to omit typing an annotation's method name, we need to name it `value`)
/// In the future, we may change it to `main`.
/// 4. **afterEach:** Scenes in this step are executed in the provided order, after each **value (or main)** scene is performed even if on a failure.
/// In this step, even if a failure happens in an **afterEach** scene, the subsequent scenes should still be executed.
/// 5. **afterAll:** Scenes in this step are executed in the provided order, after all the scenes in the **afterEach** for the last of the **value (or main)** is executed.
/// In this step, even if a failure happens in an **afterAll** scene, the subsequent scenes should still be executed.
///
/// Note that the "execution plan" and which scenes a user specifies to execute are not the same.
/// The former is modeled by `ExecutionPlan` and the latter is modeled by the `AutotestExecution.Spec`.
/// The `PlanningStrategy` instance interprets the `AutotestExecution.Spec` and creates an `ExecutionPlan`.
/// The discussion above is about the `ExecutionPlan`.
///
/// Also, a `PlanningStrategy` should be designed in a way where scenes that a user specifies explicitly are included in its resulting execution plan.
///
/// With this separation, **insdog** allows users to specify scenes that really want to execute directly.
///
/// @see AutotestExecution.Spec
/// @see PlanningStrategy
public class AutotestEngine implements BeforeAllCallback, BeforeEachCallback, TestTemplateInvocationContextProvider, AfterEachCallback, AfterAllCallback {
  private static final Logger LOGGER = LoggerFactory.getLogger(AutotestEngine.class);
  
  /// Returns `true` to let the framework know this engine supports test template.
  /// Note that test template is pre-defined as `runTestAction(String, Action)` method in `AutotestRunner` class and
  /// test programmers do not need to defined it by themselves.
  ///
  /// @param extensionContext the extension context for the test template method about to be invoked; never {@code null}
  /// @return `true`.
  @Override
  public boolean supportsTestTemplate(ExtensionContext extensionContext) {
    return true;
  }
  
  /// Returns a stream of `TestTemplateInvocationContext` objects.
  ///
  /// @param extensionContext the extension context for the test template method about to be invoked; never {@code null}
  /// @return A stream of `TestTemplateInvocationContext` objects.
  @Override
  public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext extensionContext) {
    var sceneCallMap = sceneCallMap(extensionContext);
    AtomicInteger indexHolder = new AtomicInteger(1);
    return executionPlan(extensionContext).value()
                                          .stream()
                                          .filter(sceneCallMap::containsKey)
                                          .map((String eachSceneName) -> createTestTemplateInvocationContext(extensionContext,
                                                                                                             sceneCallMap,
                                                                                                             eachSceneName,
                                                                                                             indexHolder));
  }
  
  /// Executes actions planned for **Before All** stage.
  ///
  /// @param extensionContext the current extension context; never {@code null}
  @Override
  public void beforeAll(ExtensionContext extensionContext) throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
    prepareBeforeAllStage(extensionContext, System.getProperties());
    
    AutotestRunner runner = autotestRunner(extensionContext);
    String stageName = BEFORE_ALL.stageName();
    ExecutionEnvironment executionEnvironment = createExecutionEnvironment(extensionContext, extensionContext::getDisplayName, stageName);
    configureLogging(executionEnvironment.testOutputFilenameFor("autotestExecution-beforeAll", "log"), Level.INFO);
    
    logExecutionPlan(testClass(extensionContext), executionPlan(extensionContext));
    
    Map<String, SceneCall> sceneCallMap = sceneCallMap(extensionContext);
    executionPlan(extensionContext).beforeAll()
                                   .stream()
                                   .filter(sceneCallMap::containsKey)
                                   .map((String eachSceneName) -> sceneNameToActionEntry(eachSceneName, extensionContext, executionEnvironment))
                                   .map(each -> performActionEntry(each, out -> runner.beforeAll(each.value(), runner.createWriter(out))))
                                   .filter(each -> keepRecordForPassingResult(each, passedScenesInBeforeAll(extensionContext)))
                                   .filter(AutotestEngine::hasSucceededOrThrowException)
                                   // In order to ensure all the actions are finished, accumulate the all entries into the list, first.
                                   // Then, stream again. Otherwise, the log will not become so readable.
                                   .toList()
                                   .forEach(r -> logExecutionSceneExecutionResult(r, stageName, extensionContext));
  }
  
  /// Executes actions planned for **Before Each** stage.
  ///
  /// @param executionContext the current extension context; never {@code null}
  @Override
  public void beforeEach(ExtensionContext executionContext) {
    String stageName = BEFORE_EACH.stageName();
    ExecutionEnvironment executionEnvironment = createExecutionEnvironment(executionContext,
                                                                           executionContext::getDisplayName,
                                                                           stageName);
    configureLogging(executionEnvironment.testOutputFilenameFor("autotestExecution-before", "log"), Level.INFO);
    newPassedInBeforeEach(executionContext);
    AutotestRunner runner = autotestRunner(executionContext);
    Map<String, SceneCall> sceneCallMap = sceneCallMap(executionContext);
    executionPlan(executionContext).beforeEach()
                                   .stream()
                                   .filter(sceneCallMap::containsKey)
                                   .map((String each) -> sceneNameToActionEntry(each, executionContext, executionEnvironment))
                                   .map((Entry<String, Action> each) -> performActionEntry(each, out -> runner.beforeEach(each.value(), runner.createWriter(out))))
                                   .filter((SceneExecutionResult each) -> keepRecordForPassingResult(each, passedInBeforeEach(executionContext)))
                                   .filter(AutotestEngine::hasSucceededOrThrowException)
                                   // In order to ensure all the actions are finished, accumulate the all entries into the list, first.
                                   // Then, stream again. Otherwise, the log will not become so readable.
                                   .toList()
                                   .forEach(r -> logExecutionSceneExecutionResult(r, stageName, executionContext));
    configureLogging(executionEnvironment.testOutputFilenameFor("autotestExecution-main", "log"), Level.INFO);
  }
  
  /// Executes actions planned for **After Each** stage.
  ///
  /// @param executionContext the current extension context; never {@code null}
  @Override
  public void afterEach(ExtensionContext executionContext) {
    String stageName = "afterEach";
    ExecutionEnvironment executionEnvironment = createExecutionEnvironment(executionContext,
                                                                           executionContext::getDisplayName,
                                                                           stageName);
    configureLogging(executionEnvironment.testOutputFilenameFor("autotestExecution-after", "log"), Level.INFO);
    AutotestRunner runner = autotestRunner(executionContext);
    List<ExceptionEntry> errors = new ArrayList<>();
    ExecutionPlan executionPlan = executionPlan(executionContext);
    List<String> sceneNames = Stream.concat(executionPlan.afterEach().stream(),
                                            reverse(executionPlan.beforeEach())
                                                .stream()
                                                .filter(x -> passedInBeforeEachStage(executionContext, x))
                                                .map(x -> sceneClosers(executionContext).get(x))
                                                .filter(x -> !executionPlan.afterEach().contains(x)))
                                    .toList();
    Map<String, SceneCall> sceneCallMap = sceneCallMap(executionContext);
    sceneNames.stream()
              .filter(sceneCallMap::containsKey)
              .map((String each1) -> sceneNameToActionEntry(each1, executionContext, executionEnvironment))
              .map((Entry<String, Action> each) -> performActionEntry(each, out -> runner.afterEach(each.value(),
                                                                                                    runner.createWriter(out))))
              .filter(r -> keepFailedRecordAsError(r, errors))
              // In order to ensure all the actions are finished, accumulate the all entries into the list, first.
              // Then, stream again. Otherwise, the log will not become so readable.
              .toList()
              .forEach(r -> logExecutionSceneExecutionResult(r, stageName, executionContext));
    if (!errors.isEmpty()) reportErrors(errors);
  }
  
  /// Executes actions planned for **After All** stage.
  ///
  /// @param executionContext the current extension context; never {@code null}
  @Override
  public void afterAll(ExtensionContext executionContext) {
    AutotestRunner runner = autotestRunner(executionContext);
    String stageName = AFTER_ALL.stageName();
    ExecutionEnvironment executionEnvironment = createExecutionEnvironment(executionContext,
                                                                           executionContext::getDisplayName,
                                                                           stageName);
    configureLogging(executionEnvironment.testOutputFilenameFor("autotestExecution-afterAll", "log"), Level.INFO);
    try {
      List<ExceptionEntry> errors = new ArrayList<>();
      ExecutionPlan executionPlan = executionPlan(executionContext);
      Map<String, SceneCall> sceneCallMap = sceneCallMap(executionContext);
      List<String> sceneNames = Stream.concat(executionPlan.afterAll().stream(),
                                              reverse(executionPlan.beforeAll())
                                                  .stream()
                                                  .filter(x -> passedInBeforeAllStage(executionContext, x))
                                                  .map(x -> sceneClosers(executionContext).get(x))
                                                  .filter(x -> !executionPlan.afterAll().contains(x)))
                                      .toList();
      sceneNames.stream()
                .filter(sceneCallMap::containsKey)
                .map((String each) -> sceneNameToActionEntry(each, executionContext, executionEnvironment))
                .map(each -> performActionEntry(each, out -> runner.afterAll(each.value(), runner.createWriter(out))))
                .filter(r -> keepFailedRecordAsError(r, errors))
                // In order to ensure all the actions are finished, accumulate the all entries into the list, first.
                // Then, stream again. Otherwise, the log will not become so readable.
                .toList()
                .forEach((SceneExecutionResult r) -> logExecutionSceneExecutionResult(r, stageName, executionContext));
      if (!errors.isEmpty()) reportErrors(errors);
    } finally {
      configureLoggingForSessionLevel();
    }
  }
  
  public static void configureLoggingForSessionLevel() {
    configureLogging(testResultDirectory(ExecutionEnvironment.baseLogDirectoryForTestSession(), "autotest.log"), Level.INFO);
  }
  
  public static SceneExecutionResult performActionEntry(String key, Consumer<List<String>> consumer) {
    List<String> out = new ArrayList<>();
    try {
      consumer.accept(out);
      return new SceneExecutionResult(key, null, out);
    } catch (OutOfMemoryError e) {
      // In case of `OutOfMemoryError`, nothing we can do. Just throw it to higher level.
      throw e;
    } catch (Throwable e) {
      // We are catching even `Throwable` and put in the test result.
      // Otherwise, in case we get an `Error`, it will not be reported, which isn't preferable.
      return new SceneExecutionResult(key, e, out);
    }
  }
  
  /// Creates an execution environment object for a given test class.
  ///
  /// @param testClassName A test class name for which an execution environment is created.
  /// @return An execution environment object.
  public static ExecutionEnvironment createExecutionEnvironment(String testClassName) {
    require(value(testClassName).toBe().notNull());
    return new ExecutionEnvironment() {
      @Override
      public String testClassName() {
        return testClassName;
      }
      
      @Override
      public Optional<String> testSceneName() {
        return Optional.empty();
      }
      
      @Override
      public String stepName() {
        return "unknown";
      }
    };
  }
  
  private static boolean keepRecordForPassingResult(SceneExecutionResult each, Set<String> executionContext) {
    if (each.hasSucceeded()) executionContext.add(each.name());
    return true;
  }
  
  private static boolean keepFailedRecordAsError(SceneExecutionResult r, List<ExceptionEntry> errors) {
    r.exception().ifPresent(t -> errors.add(new ExceptionEntry(r.name(), t)));
    return true;
  }
  
  private static Entry<String, Action> sceneNameToActionEntry(String sceneName, ExtensionContext executionContext, ExecutionEnvironment executionEnvironment) {
    return new Entry<>(sceneName, sceneNameToAction(sceneName, executionContext, executionEnvironment));
  }
  
  private static Action sceneNameToAction(String sceneName, ExtensionContext executionContext, ExecutionEnvironment executionEnvironment) {
    return sceneCallToAction(sceneCallMap(executionContext).get(sceneName), createActionComposer(executionEnvironment));
  }
  
  private static boolean hasSucceededOrThrowException(SceneExecutionResult sceneExecutionResult) {
    sceneExecutionResult.throwIfFailed();
    return true;
  }
  
  private static void logExecutionSceneExecutionResult(SceneExecutionResult r, String stageName, ExtensionContext executionContext) {
    LOGGER.info(r.composeMessageHeader(testClass(executionContext), stageName));
    r.out().forEach((String l) -> LOGGER.info(composeResultMessageLine(testClass(executionContext), stageName, l)));
  }
  
  private static TestTemplateInvocationContext createTestTemplateInvocationContext(ExtensionContext extensionContext,
                                                                                   Map<String, SceneCall> sceneCallMap,
                                                                                   String sceneName,
                                                                                   AtomicInteger indexHolder) {
    return new TestTemplateInvocationContext() {
      final String displayName = computeDisplayName(indexHolder.getAndIncrement());
      final ActionComposer actionComposer = createActionComposer(createExecutionEnvironment(extensionContext,
                                                                                            () -> Optional.of(displayName).orElse("XXX"),
                                                                                            "main"));
      final Action value = sceneCallToAction(sceneCallMap.get(sceneName),
                                             actionComposer);
      final List<String> coveredInMain = coveredInMain(extensionContext);
      final List<String> dependencyNames = Optional.ofNullable(executionPlan(extensionContext).dependenciesOf(sceneName))
                                                   .orElse(emptyList());
      final List<Action> d = dependencyNames.stream()
                                            .filter(n -> !coveredInMain.contains(n))
                                            .map(sceneCallMap::get)
                                            .map(s -> sceneCallToAction(s, actionComposer))
                                            .toList();
      
      @Override
      public List<Extension> getAdditionalExtensions() {
        return List.of(new ParameterResolver() {
          @Override
          public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext1) throws ParameterResolutionException {
            return parameterContext.getParameter().getType().isAssignableFrom(Action.class) || parameterContext.getParameter().getType().isAssignableFrom(String.class) || parameterContext.getParameter().getType().isAssignableFrom(Writer.class);
          }
          
          @Override
          public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext1) throws ParameterResolutionException {
            if (parameterContext.getParameter().getType().isAssignableFrom(Action.class)) {
              coveredInMain.add(sceneName);
              coveredInMain.addAll(dependencyNames);
              if (d.isEmpty())
                return value;
              if (d.size() == 1)
                return sequential(named("dependency", d.getFirst()), value);
              return sequential(named("dependencies", sequential(d)), value);
            } else if (parameterContext.getParameter().getType().isAssignableFrom(String.class)) {
              return sceneName;
            } else if (parameterContext.getParameter().getType().isAssignableFrom(Writer.class)) {
              return autotestRunner(extensionContext1).createWriter(new ArrayList<>());
            }
            throw new AssertionError();
          }
        });
      }
      
      @Override
      public String getDisplayName(int invocationIndex) {
        return computeDisplayName(invocationIndex);
      }
      
      private String computeDisplayName(int invocationIndex) {
        return TestTemplateInvocationContext.super.getDisplayName(invocationIndex) + ":" + sceneName;
      }
    };
  }
  
  private static void prepareBeforeAllStage(ExtensionContext context, Properties properties) throws
      InstantiationException,
      IllegalAccessException,
      InvocationTargetException,
      NoSuchMethodException {
    AutotestRunner runner = context.getTestInstance()
                                   .filter(AutotestRunner.class::isInstance)
                                   .map(o -> (AutotestRunner) o)
                                   .orElseThrow(RuntimeException::new);
    Class<?> accessModelClass = validateTestClass(runner.getClass());
    Map<String, Method> sceneMethodMap = sceneMethodMapOf(accessModelClass);
    Map<String, Call> sceneCallMap = sceneMethodMap.keySet()
                                                   .stream()
                                                   .map(sceneMethodMap::get)
                                                   .filter(m -> sceneMethodMap.containsKey(InternalUtils.nameOf(m)))
                                                   .map(AutotestEngine::validateSceneProvidingMethod)
                                                   .map(m -> new Entry<>(InternalUtils.nameOf(m), AutotestEngineUtils.methodToCall(m, accessModelClass, runner)))
                                                   .collect(toMap(Entry::key, Entry::value));
    
    AutotestExecution.Spec spec = loadExecutionSpec(runner, properties);
    ExecutionPlan executionPlan = planExecution(spec, sceneCallGraph(runner.getClass()), assertions(runner.getClass()));
    var closers = closers(runner.getClass());
    //NOSONAR
    assert Contracts.explicitlySpecifiedScenesAreAllCoveredInCorrespondingPlannedStage(spec, executionPlan);
    ExtensionContext.Store exetensionContextStore = exetensionContextStore(context);
    
    exetensionContextStore.put("runner", runner);
    exetensionContextStore.put("sceneCallMap", sceneCallMap);
    exetensionContextStore.put("sceneClosers", closers);
    exetensionContextStore.put("executionPlan", executionPlan);
    newPassedInBeforeAll(context);
    newCoveredInMain(context);
  }
  
  private static ExecutionEnvironment createExecutionEnvironment(ExtensionContext context, Supplier<String> displayNameSupplier, String stageName) {
    return createExecutionEnvironment(context).withDisplayName(displayNameSupplier.get(), stageName);
  }
  
  private static Map<String, Method> sceneMethodMapOf(Class<?> accessModelClass) {
    return Arrays.stream(accessModelClass.getMethods())
                 .filter(m -> m.isAnnotationPresent(Named.class))
                 .filter(m -> !m.isAnnotationPresent(Disabled.class))
                 .map(AutotestEngine::validateSceneProvidingMethod)
                 .collect(toMap(InternalUtils::nameOf, Function.identity()));
  }
  
  private static SceneExecutionResult performActionEntry(Entry<String, Action> each, Consumer<List<String>> consumer) {
    return performActionEntry(each.key(), consumer);
  }
  
  private static void logExecutionPlan(Class<?> testClass, ExecutionPlan executionPlan) {
    LOGGER.info("Running tests in: {}", testClass.getCanonicalName());
    LOGGER.info("----");
    LOGGER.info("Execution plan is as follows:");
    LOGGER.info("- beforeAll:      {}", executionPlan.beforeAll());
    LOGGER.info("- beforeEach:     {}", executionPlan.beforeEach());
    LOGGER.info("- value:          {}", executionPlan.value());
    LOGGER.info("- afterEach:      {}", executionPlan.afterEach());
    LOGGER.info("- afterAll:       {}", executionPlan.afterAll());
    LOGGER.info("----");
  }
  
  /// Checks if a scene method designated by `x` has finished normally in `beforeEach` stage.
  ///
  /// This implementation has a limitation, when a same scene is run more than once in
  /// `beforeEach` step, it cannot determine if it was finished or not correctly.
  ///
  /// @param context   A context, where `sceneName` is to be checked if executed and finished normally.
  /// @param sceneName A name of a scene method to be checked.
  /// @return `true` - finished normally (passed) / `false` - otherwise.
  private static boolean passedInBeforeEachStage(ExtensionContext context, String sceneName) {
    return passedInBeforeEach(context).contains(sceneName);
  }
  
  /// Checks if a scene method designated by `x` has finished normally in `beforeAll` stage.
  ///
  /// This implementation has a limitation, when a same scene is run more than once in
  /// `beforeAll` step, it cannot determine if it was finished or not correctly.
  ///
  /// @param context A context, where `x` is to be checked if executed and finished normally.
  /// @param x       A name of a scene method to be checked.
  /// @return `true` - finished normally (passed) / `false` - otherwise.
  private static boolean passedInBeforeAllStage(ExtensionContext context, String x) {
    return passedScenesInBeforeAll(context).contains(x);
  }
  
  private static AutotestExecution.Spec loadExecutionSpec(AutotestRunner runner, Properties properties) throws
      InstantiationException,
      IllegalAccessException,
      InvocationTargetException,
      NoSuchMethodException {
    AutotestExecution execution = runner.getClass().getAnnotation(AutotestExecution.class);
    return instantiateExecutionSpecLoader(execution).load(execution.defaultExecution(), properties);
  }
  
  private static ExecutionPlan planExecution(AutotestExecution.Spec executionSpec, Map<String, List<String>> sceneCallGraph, Map<String, List<String>> assertions) {
    return executionSpec.planExecutionWith()
                        .planExecution(executionSpec, sceneCallGraph, assertions);
  }
  
  private static Map<String, List<String>> sceneCallGraph(Class<?> accessModelClass) {
    Map<String, List<String>> sceneCallGraph = new LinkedHashMap<>();
    Arrays.stream(accessModelClass.getMethods())
          .filter(m -> m.isAnnotationPresent(Named.class))
          .filter(m -> !m.isAnnotationPresent(Disabled.class))
          .forEach(m -> {
            if (isDependencyAnnotationPresent(m)) {
              sceneCallGraph.put(InternalUtils.nameOf(m), Arrays.stream(getDependencyAnnotationValues(m)).toList());
            } else {
              sceneCallGraph.put(InternalUtils.nameOf(m), emptyList());
            }
          });
    return sceneCallGraph;
  }
  
  private static Map<String, String> closers(Class<?> accessModelClass) {
    Map<String, String> closers = new LinkedHashMap<>();
    Arrays.stream(accessModelClass.getMethods())
          .filter(m -> m.isAnnotationPresent(Named.class))
          .filter(m -> !m.isAnnotationPresent(Disabled.class))
          .forEach(m -> {
            if (m.isAnnotationPresent(ClosedBy.class)) {
              closers.put(InternalUtils.nameOf(m), m.getAnnotation(ClosedBy.class).value());
            }
          });
    return closers;
  }
  
  private static Map<String, List<String>> assertions(Class<? extends AutotestRunner> accessModelClass) {
    Map<String, List<String>> ret = new LinkedHashMap<>();
    Arrays.stream(accessModelClass.getMethods()).filter(m -> m.isAnnotationPresent(Named.class))
          .filter(m -> !m.isAnnotationPresent(Disabled.class))
          .forEach(m -> {
            if (m.isAnnotationPresent(When.class)) {
              for (String each : m.getAnnotation(When.class).value()) {
                ret.putIfAbsent(each, new LinkedList<>());
                ret.get(each).add(InternalUtils.nameOf(m));
              }
            }
          });
    return ret;
  }
  
  private static void reportErrors(List<ExceptionEntry> errors) {
    errors.forEach(each -> {
      LOGGER.error("{} {}", each.name, each.exception.getMessage());
      LOGGER.debug("{}", each.exception.getMessage(), each.exception);
    });
    throw wrap(errors.getFirst().exception);
  }
  
  private static Action sceneCallToAction(Call currentSceneCall, ActionComposer actionComposer) {
    return currentSceneCall.toAction(actionComposer);
  }
  
  private static ExecutionEnvironment createExecutionEnvironment(ExtensionContext extensionContext) {
    return createExecutionEnvironment(extensionContext.getTestClass()
                                                      .map(Class::getCanonicalName)
                                                      .orElse("Unknown-" + System.currentTimeMillis()));
  }
  
  private static Class<?> testClass(ExtensionContext extensionContext) {
    return extensionContext.getTestClass().orElseThrow(NoSuchElementException::new);
  }
  
  @SuppressWarnings("unchecked")
  private static Map<String, String> sceneClosers(ExtensionContext context) {
    return (Map<String, String>) exetensionContextStore(context).get("sceneClosers");
  }
  
  @SuppressWarnings("unchecked")
  private static List<String> coveredInMain(ExtensionContext extensionContext) {
    return (List<String>) exetensionContextStore(extensionContext).get("coveredInMain");
  }
  
  private static void newCoveredInMain(ExtensionContext extensionContext) {
    exetensionContextStore(extensionContext).put("coveredInMain", new LinkedList<>());
  }
  
  private static void newPassedInBeforeAll(ExtensionContext context) {
    exetensionContextStore(context).put("passedInBeforeAll", new HashSet<>());
  }
  
  @SuppressWarnings("unchecked")
  private static Set<String> passedScenesInBeforeAll(ExtensionContext context) {
    return (Set<String>) exetensionContextStore(context).get("passedInBeforeAll");
  }
  
  private static void newPassedInBeforeEach(ExtensionContext context) {
    exetensionContextStore(context).put("passedInBeforeEach", new HashSet<>());
  }
  
  @SuppressWarnings("unchecked")
  private static Set<String> passedInBeforeEach(ExtensionContext context) {
    return (Set<String>) exetensionContextStore(context).get("passedInBeforeEach");
  }
  
  @SuppressWarnings("unchecked")
  private static Map<String, SceneCall> sceneCallMap(ExtensionContext context) {
    return (Map<String, SceneCall>) exetensionContextStore(context).get("sceneCallMap");
  }
  
  private static ExecutionPlan executionPlan(ExtensionContext context) {
    return (ExecutionPlan) exetensionContextStore(context).get("executionPlan");
  }
  
  private static AutotestRunner autotestRunner(ExtensionContext context) {
    return (AutotestRunner) exetensionContextStore(context).get("runner");
  }
  
  private static ExtensionContext.Store exetensionContextStore(ExtensionContext context) {
    return context.getStore(ExtensionContext.Namespace.GLOBAL);
  }
  
  private static AutotestExecution.Spec.Loader instantiateExecutionSpecLoader(AutotestExecution execution) throws
      InstantiationException,
      IllegalAccessException,
      InvocationTargetException,
      NoSuchMethodException {
    return execution.executionSpecLoaderClass().getConstructor().newInstance();
  }
  
  private static <E> Class<E> validateTestClass(Class<E> aClass) {
    return aClass;
  }
  
  /// This method is called for every method `m` in a test class to be run if `m` is:
  ///
  /// * Annotated with `@Named`.
  /// * Not annotated with `@Disabled`.
  ///
  /// If all the validations are passed, method `m` itself will be returned.
  /// Otherwise, an exception will be thrown.
  ///
  /// @param m A method to be validated.
  /// @return `m` itself.
  private static Method validateSceneProvidingMethod(Method m) {
    // TODO: https://app.asana.com/0/1206402209253009/1207418182714921/f
    // @When and @Given (@DependsOn) are mutually exclusively used.
    // Methods specified by {@When,@DependsOn}#value() must be found in the class to which `m` belongs.
    // If "!" is appended to a method name in {@When,@DependsOn}#value(), the method must NOT have @ClosedBy.
    // - Because it may be performed multiple times.
    return m;
  }
  
  private static void configureLogging(Path logFilePath, Level logLevel) {
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Configuration config = ctx.getConfiguration();
    
    File logDirectory = logFilePath.getParent().toFile();
    if (logDirectory.mkdirs()) LOGGER.debug("Directory: <{}> was created for logging.", logDirectory.getAbsolutePath());
    
    PatternLayout layout = PatternLayout.newBuilder().withPattern("[%-5p] [%d{yyyy/MM/dd HH:mm:ss.SSS}] [%t] - %m%n").build();
    
    FileAppender fileAppender = FileAppender.newBuilder()
                                            .withFileName(logFilePath.toString())
                                            .withAppend(true)
                                            .withLocking(false)
                                            .setName("FileAppender")
                                            .setImmediateFlush(true)
                                            .setLayout(layout)
                                            .setConfiguration(config).build();
    fileAppender.start();
    // Create a Console Appender
    ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
                                                     .setTarget(ConsoleAppender.Target.SYSTEM_ERR)
                                                     .setName("ConsoleLogger")
                                                     .setLayout(layout)
                                                     .build();
    consoleAppender.start();
    
    
    // Remove all existing appenders
    config.getRootLogger().getAppenders().forEach((s, appender) -> config.getRootLogger().removeAppender(s));
    
    config.addAppender(fileAppender);
    config.addAppender(consoleAppender);
    
    // Add the new fileAppender
    config.getRootLogger().addAppender(fileAppender, logLevel, null);
    config.getRootLogger().addAppender(consoleAppender, logLevel, null);
    
    // Set the log level
    LoggerConfig loggerConfig = config.getRootLogger();
    loggerConfig.setLevel(logLevel);
    
    // Apply changes
    ctx.updateLoggers();
  }
  
  private record Entry<K, V>(K key, V value) {
  }
  
  /// This record models an execution plan created from the requirement given by user.
  ///
  /// The framework executes the scenes returned by each method.
  /// It is a design concern of the test engine (`AutotestEngine`) how scenes within the stage.
  /// For instance, whether they should be executed sequentially or concurrently, although sequential execution will be preferred in most cases.
  /// The engine should execute each state as an instance of this record gives.
  /// All scenes in `beforeAll` should be executed in the `beforeAll` stage, nothing else at all, in the order, where they are returned,
  /// as long as they give no errors, and as such.
  ///
  /// In situations, where a non-directly required scenes need to be executed for some reason (E.g., a scene in a stage requires some others to be executed beforehand),
  /// including the scenes implicitly and sorting out the execution order appropriately is the responsibility of the `PlanningStrategy`, not the engine.
  ///
  /// @param beforeAll    The names of the scenes to be executed in the `beforeAll` scene.
  /// @param beforeEach   The names of the scenes to be executed in the `beforeEach` scene.
  /// @param value        The names of the scenes to be executed as real tests.
  /// @param dependencies A map that holds dependencies of a given scene in main.
  /// @param afterEach    The names of the scenes to be executed in the `afterEach` scene.
  /// @param afterAll     The names of the scenes to be executed in the `afterAll` scene.
  /// @see AutotestEngine
  /// @see PlanningStrategy
  public record ExecutionPlan(List<String> beforeAll, List<String> beforeEach,
                              List<String> value,
                              Map<String, List<String>> dependencies,
                              List<String> afterEach, List<String> afterAll) {
    /**
     * @param sceneName A scene name in main (`value` list)
     * @return A list of dependencies of a given `sceneName`.
     * Note that only dependencies in `value` list will be returned in the list.
     */
    List<String> dependenciesOf(String sceneName) {
      return dependencies.get(sceneName);
    }
  }
  
  /// A class that models a result of a scene execution.
  public static class SceneExecutionResult {
    private final String name;
    private final Throwable exception;
    private final List<String> out;
    
    SceneExecutionResult(String name, Throwable exception, List<String> out) {
      this.name = requireNonNull(name);
      this.exception = exception;
      this.out = requireNonNull(out);
    }
    
    /// Returns a name of this object.
    ///
    /// @return A name
    public String name() {
      return this.name;
    }
    
    public Optional<Throwable> exception() {
      return Optional.ofNullable(this.exception);
    }
    
    public boolean hasSucceeded() {
      return this.exception == null;
    }
    
    public void throwIfFailed() {
      if (hasSucceeded()) return;
      throw wrapIfNecessary(this.exception);
    }
    
    public List<String> out() {
      return unmodifiableList(this.out);
    }
    
    private String composeMessageHeader(Class<?> testClass, String stageName) {
      return composeMessageHeader(testClass, this, stageName);
    }
    
    private static String composeMessageHeader(Class<?> testClass, SceneExecutionResult r, String stageName) {
      return format("%-20s: %-11s [%1s]%-20s %-40s",
                    testClass.getSimpleName(),
                    stageName + ":",
                    r.hasSucceeded() ? "o" : "E",
                    r.name(),
                    r.exception()
                     .map(Throwable::getMessage)
                     .orElse(""));
    }
  }
  
  record ExceptionEntry(String name, Throwable exception) {
  }
  
  enum Stage {
    BEFORE_ALL("beforeAll"),
    BEFORE_EACH("beforeEach"),
    MAIN("value"),
    AFTER_EACH("afterEach"),
    AFTER_ALL("afterAll");
    
    private final String stageName;
    
    Stage(String stageName) {
      this.stageName = stageName;
    }
    
    String stageName() {
      return this.stageName;
    }
  }
  
  enum Contracts {
    ;
    
    private static boolean explicitlySpecifiedScenesAreAllCoveredInCorrespondingPlannedStage(AutotestExecution.Spec spec, ExecutionPlan executionPlan) {
      return all(plannedScenesCoverAllSpecifiedScenes(spec,
                                                      specifiedScenesInStage(BEFORE_ALL.stageName(),
                                                                             (AutotestExecution.Spec v) -> asList(v.beforeAll())),
                                                      predicatePlannedScenesContainsSpecifiedScene(BEFORE_ALL.stageName(),
                                                                                                   executionPlan.beforeAll())),
                 plannedScenesCoverAllSpecifiedScenes(spec,
                                                      specifiedScenesInStage(BEFORE_EACH.stageName(),
                                                                             (AutotestExecution.Spec v) -> asList(v.beforeEach())),
                                                      predicatePlannedScenesContainsSpecifiedScene(BEFORE_EACH.stageName(),
                                                                                                   executionPlan.beforeEach())),
                 plannedScenesCoverAllSpecifiedScenes(spec,
                                                      specifiedScenesInStage(MAIN.stageName(),
                                                                             (AutotestExecution.Spec v) -> asList(v.value())),
                                                      predicatePlannedScenesContainsSpecifiedScene(MAIN.stageName(),
                                                                                                   executionPlan.value())),
                 plannedScenesCoverAllSpecifiedScenes(spec,
                                                      specifiedScenesInStage(AFTER_EACH.stageName(),
                                                                             (AutotestExecution.Spec v) -> asList(v.afterEach())),
                                                      predicatePlannedScenesContainsSpecifiedScene(AFTER_EACH.stageName(),
                                                                                                   executionPlan.afterEach())),
                 plannedScenesCoverAllSpecifiedScenes(spec,
                                                      specifiedScenesInStage(AFTER_ALL.stageName(),
                                                                             (AutotestExecution.Spec v) -> asList(v.afterAll())),
                                                      predicatePlannedScenesContainsSpecifiedScene(AFTER_ALL.stageName(),
                                                                                                   executionPlan.afterAll())));
    }
    
    private static Predicate<String> predicatePlannedScenesContainsSpecifiedScene(String stageName, List<String> plannedScenes) {
      return MakePrintable.<String>predicate(plannedScenes::contains).$("executionPlan." + stageName + ".contains");
    }
    
    private static Function<AutotestExecution.Spec, List<String>> specifiedScenesInStage(String stageName,
                                                                                         Function<AutotestExecution.Spec, List<String>> scenesInStage) {
      return MakePrintable.function(scenesInStage).$("spec." + stageName);
    }
    
    private static Statement<AutotestExecution.Spec> plannedScenesCoverAllSpecifiedScenes(AutotestExecution.Spec spec,
                                                                                          Function<AutotestExecution.Spec, List<String>> specifiedScenesInSpec,
                                                                                          Predicate<String> plannedScenesContains) {
      return value(spec).function(specifiedScenesInSpec)
                        .asListOf(String.class).stream()
                        .toBe()
                        .allMatch(plannedScenesContains);
    }
  }
}