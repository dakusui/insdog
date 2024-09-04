package jp.co.moneyforward.autotest.ca_web.accessmodels;

import com.github.dakusui.actionunit.core.Context;
import com.github.dakusui.actionunit.core.Context.Impl;
import com.github.dakusui.actionunit.visitors.ReportingActionPerformer;
import com.microsoft.playwright.*;
import jp.co.moneyforward.autotest.actions.web.*;
import jp.co.moneyforward.autotest.ca_web.core.ExecutionProfile;
import jp.co.moneyforward.autotest.framework.action.LeafAct.Func;
import jp.co.moneyforward.autotest.framework.action.LeafAct.Let;
import jp.co.moneyforward.autotest.framework.action.Scene;
import jp.co.moneyforward.autotest.framework.annotations.ClosedBy;
import jp.co.moneyforward.autotest.framework.annotations.DependsOn;
import jp.co.moneyforward.autotest.framework.annotations.Export;
import jp.co.moneyforward.autotest.framework.annotations.Named;
import jp.co.moneyforward.autotest.framework.core.AutotestRunner;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static jp.co.moneyforward.autotest.actions.web.PageFunctions.*;

/**
 * This accessing model assumes that the user provided by `EXECUTION_PROFILE` has already been registered and associated with
 * a corporation to which they belong.
 *
 * Following is a list of attributes that this accessing model requires
 *
 * - **User:** `ukai.hiroshi+autotest1@moneyforward.co.jp`
 * - **Password:** `!QAZ@WSX`
 * - **Company name(会社名):** スペシャルサンドボックス合同会社
 *
 * Following is a list of attributes used for executing tests with this access model in the staging environment
 * Those are not necessarily ths same as in a new environment in which you are going to use this access model.
 * However, tests implemented in the future may depend on them.
 *
 * - **Phone number(電話番号):** 08012345678
 * - **Prefecture(都道府県):** Tokyo(東京都)
 * - **Fiscal Year(会計期間):** 4/1-3/31
 * - **Taxation Method(課税方式):** Taxed by default (Case-by-case method) 原則課税(個別対応方式)
 *
 * This is an accessing model for *ca_web* to implement [駄犬くん](https://github.com/moneyforward/ca_web_e2e_test_d) and further advanced tests.
 *
 * **NOTE:**
 * Currently, as a library `osynth` that **autotest-ca** uses doesn't respect Java's module system, you need to pass the
 * following JVM option.:
 *
 * `--add-opens java.base/java.lang.invoke=ALL-UNNAMED`
 */
@SuppressWarnings("JavadocLinkAsPlainText")
public class CawebAccessingModel implements AutotestRunner {
  /**
   * An execution profile, which hosts variables in test executions.
   *
   * The variables in a profile should not change their values in one test execution.
   */
  private static final ExecutionProfile executionProfile = ExecutionProfile.create();
  
  private final ReportingActionPerformer actionPerformer = new ReportingActionPerformer(getContext(), new HashMap<>());
  
  public static ExecutionProfile executionProfile() {
    return executionProfile;
  }
  
  /**
   * Returns a scene that performs **open** operation.
   *
   * The returned scene should be closed by **close** operation.
   * This is a general action, that can be used for various applications because it internally does only create **Playwright**
   * instance, open a **Browser** object, and then create a new **Browser** context, all of which are necessary to almost all
   * web applications.
   *
   * @return A scene that performs **open** operation.
   */
  @Named
  @ClosedBy("close")
  @Export({"window", "browser", "page"})
  public static Scene open() {
    TimeUnit timeUnit = SECONDS;
    int time = 30;
    String windowVariableName = "window";
    String browserVariableName = "browser";
    return new Scene.Builder("NONE")
        .add(windowVariableName, new Let<>(Playwright.create()))
        .add(browserVariableName,
             new Func<>("Playwright::chromium",
                        (Playwright playwright) -> launchBrowser(playwright.chromium(),
                                                                 executionProfile())),
             windowVariableName)
        .add("browserContext", new Func<>("Browser::newContext->setDefaultTimeout(" + time + timeUnit + ")", (Browser b) -> {
          BrowserContext c = browserContextFrom(b, executionProfile());
          c.setDefaultTimeout(timeUnit.toMillis(time));
          return c;
        }), browserVariableName)
        .add("page", new Func<>("BrowserContext::newPage[1440x900]", (BrowserContext browserContext) ->  {
          Page page = browserContext.newPage();
          page.setViewportSize(1440, 900);
          return page;
        }), "browserContext")
        .build();
  }
  
  /**
   * Returns an action for logging in the **ca_web** application using the variables defined in the `EXECUTION_PROFILE`.
   *
   * @return A login action.
   * @see CawebAccessingModel#executionProfile
   */
  @Named
  @ClosedBy("logout")
  @Export("page")
  @DependsOn("open")
  public static Scene login() {
    return new Scene.Builder("page")
        .add(new Navigate(executionProfile.homeUrl()))
        //       Clicking "ログインはこちら" button, which is displayed only in idev, if any.
        .add(new ClickIfPresent(locatorBySelector("#simple-layout > div.main-container > div > div.text-center > a")))
        .add(new SendKey(PageFunctions.locatorByPlaceholder("example@moneyforward.com"), executionProfile.userEmail()))
        .add(new Click(buttonLocatorByName("ログインする")))
        .add(new SendKey(PageFunctions.locatorByLabel("パスワード"), executionProfile.userPassword()))
        .add(new Click("button[id='submitto']"))
        .add(new SendKey(PageFunctions.locatorByPlaceholder("000000"), executionProfile.totpForNow()))
        .add(new Click(PageFunctions.buttonLocatorByName("認証する")))
        .build();
  }
  
  /**
   * Returns a scene, which performs a "logout" action.
   *
   * @return A scene to perform a "logout" action.
   */
  @Named
  @Export("page")
  @DependsOn("login")
  public static Scene logout() {
    return new Scene.Builder("page")
        .add(new Click(linkLocatorByName(") ", true)))
        .add(new Click(linkLocatorByName("ログアウト")))
        .build();
  }
  
  /**
   * Returns a scene object, which performs a screenshot.
   *
   * @return A scene object which performs a screenshot.
   * @see Screenshot
   */
  @Named
  @Export("page")
  @DependsOn("open")
  public static Scene screenshot() {
    return new Scene.Builder("page")
        .add("NONE", new Screenshot(), "page")
        .build();
  }
  
  /**
   * Returns a `Scene`, which closes the ongoing session.
   *
   * @return A `Scene`, which closes the ongoing session.
   */
  @Named
  @DependsOn("open")
  public static Scene close() {
    return new Scene.Builder("close")
        .add("NONE", new CloseBrowser(), "browser")
        .add("NONE", new CloseWindow(), "window")
        .build();
  }
  
  /**
   * Creates a browser context object from the browser `b`.
   *
   * @param b                A browser.
   * @param executionProfile An execution profile.
   * @return A browser context object.
   */
  public static BrowserContext browserContextFrom(Browser b, ExecutionProfile executionProfile) {
    return b.newContext(new Browser.NewContextOptions()
                            .setLocale(executionProfile.locale()));
  }
  
  /**
   * Returns an `ActionPerformer`, with which actions created from `Scene` are performed.
   *
   * @return An action performer object.
   */
  @Override
  public ReportingActionPerformer actionPerformer() {
    return actionPerformer;
  }
  
  public static Context getContext() {
    return new Impl() {
      @Override
      public Context createChild() {
        return this;
      }
    };
  }
  
  
  public static Browser launchBrowser(BrowserType browserType, ExecutionProfile executionProfile) {
    boolean headless = executionProfile.setHeadless();
    if (headless)
      LOGGER.info("HEADLESS MODE");
    else
      LOGGER.info("HEADFUL MODE");
    return browserType.launch(new BrowserType.LaunchOptions()
                                  .setHeadless(headless));
  }
}
