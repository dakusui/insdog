package jp.co.moneyforward.autotest.ut.framework.utils;

import jp.co.moneyforward.autotest.framework.core.AutotestException;
import jp.co.moneyforward.autotest.framework.utils.InsdogUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static com.github.valid8j.fluent.Expectations.assertStatement;
import static com.github.valid8j.fluent.Expectations.value;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InsdogUtilsTest {
  @Test
  void givenNonExistingResourcePath_whenMaterializeResource_thenExceptionThrown() throws IOException {
    var resourcePath = "not/existing/image.png";
    File parentFile = File.createTempFile("test", "test").getParentFile();
    
    AutotestException e = assertThrows(AutotestException.class, () -> {
      InsdogUtils.materializeResource(parentFile, resourcePath);
    });
    assertStatement(value(e).getMessage().toBe().containing(resourcePath));
  }
}
