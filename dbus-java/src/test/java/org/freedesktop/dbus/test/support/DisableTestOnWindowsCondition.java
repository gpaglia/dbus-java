package org.freedesktop.dbus.test.support;

import org.freedesktop.dbus.utils.Util;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisableTestOnWindowsCondition implements ExecutionCondition {
  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    if(Util.isWindows()) {
      return ConditionEvaluationResult.disabled("Test disabled on Windows");
    } else {
      return ConditionEvaluationResult.enabled("Test enabled");
    }
  }
}
