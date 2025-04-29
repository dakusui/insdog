package jp.co.moneyforward.autotest.framework.utils;

import com.github.dakusui.actionunit.core.Action;
import com.github.dakusui.actionunit.core.Context;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

///
/// This class is deprecated and kept only the sake for compatibility.
/// This class will be removed in the next major version.
/// @deprecated Use {@link jp.co.moneyforward.autotest.framework.internal.InternalUtils} instead.
///
@Deprecated(forRemoval = true)
public enum InternalUtils {
  ;
  
  ///
  /// Returns an `Optional` of a `String` that contains a branch name.
  /// This method internally calls `InternalUtils#currentBranchNameFor(new File("."))`.
  ///
  /// @return An `Optional` of branch name `String`.
  /// @see InternalUtils#currentBranchNameFor(File)
  ///
  @Deprecated
  public static Optional<String> currentBranchName() {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.currentBranchName();
  }
  
  ///
  /// Returns an `Optional` of a `String` that contains a branch name, if the given `projectDir` has `.git` directory and a current branch name of it can be retrieved.
  /// An exception will be thrown on a failure during this step.
  ///
  /// Otherwise, an empty `Optional` will be returned.
  ///
  /// @return An `Optional` of branch name `String`.
  ///
  @Deprecated
  public static Optional<String> currentBranchNameFor(File projectDir) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.currentBranchNameFor(projectDir);
  }
  
  @Deprecated
  public static boolean isPresumablyRunningFromIDE() {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.isPresumablyRunningFromIDE();
  }
  
  @Deprecated
  public static boolean isRunUnderSurefire() {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.isRunUnderSurefire();
  }
  
  @Deprecated
  public static boolean isRunUnderPitest() {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.isRunUnderPitest();
  }
  
  @Deprecated
  public static String composeResultMessageLine(Class<?> testClass, String stageName, String line) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.composeResultMessageLine(testClass, stageName, line);
  }
  
  @Deprecated
  public static File projectDir() {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.projectDir();
  }
  
  @Deprecated
  public static String simpleClassNameOf(Class<?> clazz) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.simpleClassNameOf(clazz);
  }
  
  @Deprecated
  public static Stream<Action> flattenIfSequential(Action a) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.flattenIfSequential(a);
  }
  
  ///
  /// A shorthand method of `shorten(string, 120)`.
  ///
  /// @param string A string to be shortened.
  /// @return A shortened string.
  ///
  @Deprecated
  public static String shorten(String string) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.shorten(string);
  }
  
  ///
  /// Shorten a `string` to the specified `length`.
  /// In case `string` contains  a carriage return (`\r`), a substring from the beginning of the `string` to the position
  /// of the character will be returned.
  ///
  /// @param string A string to be shortened.
  /// @param length A length to which `string` to be shortened.
  /// @return A shortened string.
  ///
  @Deprecated
  public static String shorten(String string, int length) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.shorten(string, length);
  }
  
  @Deprecated
  public static String mask(Object o) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.mask(o);
  }
  
  @Deprecated
  public static Stream<Action> flattenSequentialAction(Action action) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.flattenIfSequential(action);
  }
  
  // NOSONAR: Intrusive warning. Number of hierarchical depth should not be checked against very well known library such as opentest4j
  @Deprecated
  public static class AssumptionViolation extends jp.co.moneyforward.autotest.framework.internal.InternalUtils.AssumptionViolation {
    public AssumptionViolation(String message) {
      super(message);
    }
  }
  
  ///
  /// Creates a `Date` object from a string formatted with `MMM/dd/yyyy`.
  /// `Locale.US` is used to create a `SimpleDateFormat` object.
  ///
  /// @param dateString A string from which a `Date` object is created.
  /// @return A date object created from `dateString`.
  ///
  @Deprecated
  public static Date date(String dateString) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.date(dateString);
  }
  
  ///
  /// Returns a `Date` object from the current date.
  ///
  /// @return A date object created from the current date.
  ///
  @Deprecated
  public static Date now() {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.now();
  }

  @Deprecated
  public static String dateToSafeString(Date date) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.dateToSafeString(date);
  }
  
  ///
  /// Concatenates given streams.
  ///
  /// @param streams Streams to be concatenated.
  /// @param <T>     The type of the values streamed by the given `streams`.
  /// @return Concatenated stream.
  ///
  @SafeVarargs
  @Deprecated
  public static <T> Stream<T> concat(Stream<T>... streams) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.concat(streams);
  }
  
  ///
  /// Returns an action context for InsDog.
  /// The returned context is designed to print a proper message when each value in the action context is a variable store.
  ///
  /// @return A created context.
  ///
  @Deprecated
  public static Context createContext() {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.createContext();
  }
  
  ///
  /// Creates a consumer, which gives a `consumerName`, when `toString` method is called.
  ///
  /// @param consumerName A name of the created consumer. Returned from `toString`.
  /// @param consumer     A consumer from which the returned object is created.
  /// @return A consumer which executes the `accept` method of the consumer and returns `consumerName` for `toString`.
  ///
  @Deprecated
  public static Consumer<Context> printableConsumer(final String consumerName, Consumer<Context> consumer) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.printableConsumer(consumerName, consumer);
  }
  
  ///
  /// Creates a leaf action, which executes the `accept` method of `contextConsumer`.
  /// Inside this method, the given `contextConsumer` method is made printable using the `printableConsumer` method.
  /// Then it will be passed to `ActionSupport#leaf` method to turn it into an action.
  ///
  /// @param name            A name of the action.
  /// @param contextConsumer A consumer to define the behavior of the returned action.
  /// @return A leaf action created from the `contextConsumer`.
  ///
  @Deprecated
  public static Action action(String name, Consumer<Context> contextConsumer) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.action(name, contextConsumer);
  }
  
  ///
  /// Creates a trivial leaf action, which is the same as an action created by `InternalUtils.action(String, Consumer<Context>)`.
  ///
  /// @param name            A name of the action.
  /// @param contextConsumer A consumer that defines the behavior of the action.
  @Deprecated
  public static Action trivialAction(String name, Consumer<Context> contextConsumer) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.trivialAction(name, contextConsumer);
  }
  
  ///
  /// Returns a predicate that tests if the date given to it is after the `date`.
  ///
  /// @param date The returned predicate returns `true` if a given date is after this.
  /// @return A predicate to check if a given date is after `date`.
  ///
  @Deprecated
  public static Predicate<Date> dateAfter(Date date) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.dateAfter(date);
  }
  
  ///
  /// Checks if the given `object` has a `toString` method which overrides `Object#toString`.
  ///
  /// @param object An object to be checked.
  /// @return `true` - `toString` method is overridden / `false` - otherwise.
  ///
  @Deprecated
  public static boolean isToStringOverridden(Object object) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.isToStringOverridden(object);
  }
  
  ///
  /// // @formatter:off
  /// Wraps a given exception `e` with a framework specific exception, `AutotestException`.
  ///
  /// This method has `RuntimeException` as return value type, however, this method will never return a value but throws an exception.
  /// The return type is defined to be able to write a caller code in the following style, which increases readability.
  ///
  /// ```java
  /// try {
  ///   doSomthing()
  /// } catch (SomeCheckedException e) {
  ///   throw wrap(e);
  /// }
  /// ```
  ///
  /// If a given exception `e` is a `RuntimeException`, or an `Error`, it will not be wrapped, but `e` will be directly thrown.
  ///
  /// // @formatter:on
  ///
  /// @param e An exception to be wrapped.
  /// @return This method will never return any value.
  ///
  public static RuntimeException wrap(Throwable e) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.wrap(e);
  }
  
  ///
  /// Write a given `text` to a `file`.
  /// When the `file` already exists, `text` will be appended to it.
  /// `text` will be encoded into `UTF-8` since this method calls `Files.writeString(Path,String,OpenOption...)` internally.
  ///
  /// In case the `file` doesn't exist or its parent directories don't exist, this function will try to create them.
  ///
  /// On a failure, a runtime exception will be thrown.
  ///
  /// @param file A file to which `text` is written to.
  /// @param text A data to be written.
  ///
  public static void writeTo(File file, String text) {
    jp.co.moneyforward.autotest.framework.internal.InternalUtils.writeTo(file, text);
  }
  
  ///
  /// Removes a given `file`, if exists.
  /// If it doesn't exist, this method does nothing.
  /// If the `file` is a directory, it must be empty.
  /// Otherwise, an exception will be thrown.
  ///
  /// @param file A file to be deleted.
  ///             Must not be `null`.
  ///
  @Deprecated
  public static void removeFile(File file) {
    jp.co.moneyforward.autotest.framework.internal.InternalUtils.removeFile(file);
  }
  
  @Deprecated
  public static <T> List<T> reverse(List<T> list) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.reverse(list);
  }
  
  @Deprecated
  public record Entry<K, V>(K key, V value) {
    @Deprecated
    public static <K, V> Entry<K, V> $(K key, V value) {
      return new Entry<>(key, value);
    }
  }
  
  ///
  /// Copies the contents of a resource file from the classpath to a temporary file
  ///
  /// @param resourcePath A path to a resource on a class path to be materialized
  /// @return a temporary file path containing the contents of the resource
  ///
  @Deprecated
  public static File materializeResource(String resourcePath) {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.materializeResource(resourcePath);
  }
  
  ///
  /// Copies the contents of a resource file from the classpath to a specified output file.
  ///
  /// @param output       The output file to which the resource contents will be written
  /// @param resourcePath A path to a resource on a class path to be materialized
  ///
  @Deprecated
  public static void materializeResource(File output, final String resourcePath) {
    jp.co.moneyforward.autotest.framework.internal.InternalUtils.materializeResource(output, resourcePath);
  }
  
  @Deprecated
  public static void copyTo(InputStream in1, OutputStream out1) {
    jp.co.moneyforward.autotest.framework.internal.InternalUtils.copyTo(in1, out1);
  }
  
  @Deprecated
  public static File temporaryDirectory() {
    return jp.co.moneyforward.autotest.framework.internal.InternalUtils.temporaryDirectory();
  }
  
  @Deprecated
  public static class TrivialAction extends jp.co.moneyforward.autotest.framework.internal.InternalUtils.TrivialAction {
    public TrivialAction(Consumer<Context> consumer) {
      super(consumer);
    }
  }
}
