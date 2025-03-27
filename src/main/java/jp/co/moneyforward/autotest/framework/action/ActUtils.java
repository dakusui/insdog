package jp.co.moneyforward.autotest.framework.action;

import com.microsoft.playwright.Page;

import java.util.function.Consumer;
import java.util.function.Function;

import static jp.co.moneyforward.autotest.framework.utils.InternalUtils.isToStringOverridden;

public enum ActUtils {
    ;

    public static <T> Act.Let<T> let(T value) {
        return new Act.Let<>(value);
    }

    public static <T, R> Act.Func<T, R> func(Function<T, R> func) {
        return new Act.Func<>(isToStringOverridden(func) ? func.toString()
                                      : "func", func);
    }

    public static Act.Func<Page, Page> page(Consumer<Page> action) {
        return func((Page page) -> {
            action.accept(page);
            return page;
        });
    }
}
