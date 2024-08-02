package jp.co.moneyforward.autotest.it;

import jp.co.moneyforward.autotest.ca_web.cli.Cli;
import jp.co.moneyforward.autotest.framework.cli.CliUtils;
import jp.co.moneyforward.autotest.ututils.TestBase;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import picocli.CommandLine;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.github.valid8j.fluent.Expectations.*;
import static com.github.valid8j.pcond.forms.Printables.function;

public class CliIT extends TestBase {
  
  @Test
  public void testHelp() {
    int exitCode = new CommandLine(new Cli()).setExecutionStrategy(new NoExitExecutionStrategy())
                                             .execute("--help");
    
    assertStatement(value(exitCode).toBe().equalTo(0));
  }
  
  @Test
  public void testHelpFrom_Cli$main() {
    Cli.main("--help");
    // Always passing as long as it reaches here as the exit code is already checked by `testHelp` test method.
    assertStatement(value(true).toBe().equalTo(true));
  }
  
  @Test
  public void runSelfTest() {
    int exitCode = new CommandLine(new Cli()).setExecutionStrategy(new NoExitExecutionStrategy())
                                             .execute("-q", "classname:~.*SelfTest.*", "run");
    
    assertStatement(value(exitCode).toBe().equalTo(0));
  }
  
  @Test
  public void runListTestClasses() {
    int exitCode = new CommandLine(new Cli()).setExecutionStrategy(new NoExitExecutionStrategy())
                                             .execute("list-testclasses");
    
    assertStatement(value(exitCode).toBe().equalTo(0));
  }
  
  @Test
  public void runListTagsWithInvalidArgs() {
    int exitCode = new CommandLine(new Cli()).setExecutionStrategy(new NoExitExecutionStrategy())
                                             .execute("-q", "classname??.*", "list-testclasses");
    System.out.println(exitCode);
    assertStatement(value(exitCode).toBe().not(c -> c.equalTo(2)));
  }
  
  @Test
  public void runListTags() {
    int exitCode = new CommandLine(new Cli()).setExecutionStrategy(new NoExitExecutionStrategy())
                                             .execute("list-tags");
    
    assertStatement(value(exitCode).toBe().equalTo(0));
  }
  
  /**
   * This test just checks if the CommandLine#execute finishes without an error.
   */
  @Test
  public void runSelfTestWithPartialMatch() {
    int exitCode = new CommandLine(new Cli()).setExecutionStrategy(new NoExitExecutionStrategy())
                                             .execute("-q", "classname:%SelfTest", "run");
    
    assertStatement(value(exitCode).toBe().equalTo(0));
  }
  
  
  @Test
  public void runSelfTestThroughCliUtils() {
    List<TestIdentifier> testIdentifiers = new LinkedList<>();
    
    Map<Class<?>, TestExecutionSummary> testReport = CliUtils.runTests(
        "jp.co.moneyforward.autotest.ca_web.tests",
        new String[]{"classname:%SelfTest"}, new String[]{},
        new String[]{},
        new SummaryGeneratingListener() {
          @Override
          public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            testIdentifiers.add(testIdentifier);
            super.executionFinished(testIdentifier, testExecutionResult);
          }
        });
    
    int numFailures = testReport.values()
                                .stream()
                                .map(s -> s.getFailures().size())
                                .reduce(Integer::sum)
                                .orElseThrow(NoSuchElementException::new);
    assertAll(value(numFailures).toBe().equalTo(0),
              value(testIdentifiers).elementAt(0)
                                    .function(function("getDisplayName", TestIdentifier::getDisplayName))
                                    .asString()
                                    .satisfies()
                                    .containing("connect"),
              value(testIdentifiers).elementAt(1)
                                    .function(function("getDisplayName", TestIdentifier::getDisplayName))
                                    .asString()
                                    .satisfies()
                                    .containing("disconnect"));
  }
  
  @Test
  public void runFailingTestThroughCliUtils() {
    List<TestIdentifier> testIdentifiers = new LinkedList<>();
    
    Map<Class<?>, TestExecutionSummary> testReport = CliUtils.runTests(
        "jp.co.moneyforward.autotest.it.t4t",
        new String[]{"classname:%Failing"}, new String[]{},
        new String[]{},
        new SummaryGeneratingListener() {
          @Override
          public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            testIdentifiers.add(testIdentifier);
            super.executionFinished(testIdentifier, testExecutionResult);
          }
        });
    
    int numFailures = testReport.values()
                                .stream()
                                .map(s -> s.getFailures().size())
                                .reduce(Integer::sum)
                                .orElseThrow(NoSuchElementException::new);
    List<String> failedTests = testReport.values()
                                         .stream()
                                         .flatMap((TestExecutionSummary v) -> v.getFailures().stream())
                                         .map(TestExecutionSummary.Failure::getTestIdentifier)
                                         .map(TestIdentifier::getDisplayName).toList();
    assertAll(value(numFailures).toBe().equalTo(1),
              value(failedTests).elementAt(0).asString().toBe().containing("fail"));
  }
  
  static class NoExitExecutionStrategy implements CommandLine.IExecutionStrategy {
    @Override
    public int execute(CommandLine.ParseResult parseResult) {
      return new CommandLine.RunLast() {
        @Override
        public int execute(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException {
          return super.execute(parseResult);
        }
        
      }.execute(parseResult);
    }
  }
}
