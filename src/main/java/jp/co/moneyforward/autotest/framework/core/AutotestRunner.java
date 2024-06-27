package jp.co.moneyforward.autotest.framework.core;

import com.github.dakusui.actionunit.core.Action;
import com.github.dakusui.actionunit.io.Writer;
import com.github.dakusui.actionunit.visitors.ReportingActionPerformer;
import jp.co.moneyforward.autotest.framework.annotations.AutotestExecution;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutotestExecution
public interface AutotestRunner {
  Logger LOGGER = LoggerFactory.getLogger(AutotestRunner.class);
  
  default void beforeAll(Action action) {
    performAction(action);
  }
  
  default void beforeEach(Action action) {
    performAction(action);
  }
  
  @TestTemplate
  default void runTestAction(Action action) {
    performActionWithReporting(action);
  }
  
  default void afterEach(Action action) {
    performAction(action);
  }
  
  default void afterAll(Action action) {
    performAction(action);
  }
  
  default void performActionWithReporting(Action action) {
    actionPerformer().performAndReport(action, createWriter());
  }
  
  ReportingActionPerformer actionPerformer();
  
  default void performAction(Action action) {
    try {
      action.accept(actionPerformer());
    } catch (RuntimeException | Error e) {
      LOGGER.error(e.getMessage(), e);
      throw e;
    }
  }
  
  default Writer createWriter() {
    return Writer.Slf4J.INFO::writeLine;
  }
}
