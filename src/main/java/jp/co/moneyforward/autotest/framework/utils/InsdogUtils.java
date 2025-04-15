package jp.co.moneyforward.autotest.framework.utils;

import com.microsoft.playwright.Page;
import jp.co.moneyforward.autotest.framework.action.Act;
import jp.co.moneyforward.autotest.framework.internal.InternalUtils;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Function;

import static jp.co.moneyforward.autotest.framework.internal.InternalUtils.isToStringOverridden;

public enum InsdogUtils {
  ;
  
  /// Copies the contents of a resource file from the classpath to a specified output file.
  ///
  /// @param output       The output file to which the resource contents will be written
  /// @param resourcePath A path to a resource on a class path to be materialized
  public static void materializeResource(File output, String resourcePath) {
    InternalUtils.materializeResource(output, resourcePath);
  }
  
  public static <T> Act.Let<T> let(T value) {
    return new Act.Let<>(value);
  }
  
  public static <T, R> Act.Func<T, R> func(Function<T, R> func) {
    return new Act.Func<>(isToStringOverridden(func) ? func.toString()
                                                     : "func", func);
  }
  
  public static <T> Act.Sink<T> sink(Consumer<T> sink) {
    return new Act.Sink<>(isToStringOverridden(sink) ? sink.toString()
                                                     : "sink", sink);
  }
  
  public static Act.Func<Page, Page> page(Consumer<Page> action) {
    return func((Page page) -> {
      action.accept(page);
      return page;
    });
  }
}
