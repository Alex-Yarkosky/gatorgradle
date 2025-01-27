package org.gatorgradle.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gatorgradle.GatorGradlePlugin;
import org.gatorgradle.command.BasicCommand;
import org.gatorgradle.command.Command;
import org.gatorgradle.command.GatorGraderCommand;
import org.gatorgradle.config.GatorGradleConfig;
import org.gatorgradle.task.GatorGradleTask;
import org.gatorgradle.util.StringUtil;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

public class CommandOutputSummary {
  private static final String YES = StringUtil.color(StringUtil.GOOD, "Yes");
  private static final String NO = StringUtil.color(StringUtil.BAD, "No");

  private List<Command> completedCommands;
  private final Logger log;

  public CommandOutputSummary(Logger log) {
    this.completedCommands = new ArrayList<>();
    this.log = log;
  }

  public CommandOutputSummary(List<Command> completedCommands, Logger log) {
    this.completedCommands = new ArrayList<>(completedCommands);
    this.log = log;
  }

  /**
   * Add the command to the summary.
   *
   * @param cmd the command to add
   */
  public void addCompletedCommand(Command cmd) {
    if (!completedCommands.contains(cmd)) {
      completedCommands.add(cmd);
      showInProgressSummary(cmd);
    } else {
      log.info("Duplicate command: " + cmd.toString());
    }
  }

  public int getNumCompletedTasks() {
    return completedCommands.size();
  }

  boolean nomore = false;

  /**
   * Output a description of what just finished, or maybe a status.
   *
   * @param cmd the command that just finished
   */
  public void showInProgressSummary(Command cmd) {
    if (nomore) {
      return;
    }
    boolean fail = printCommandResult(cmd, false);
    if (fail && GatorGradleConfig.get().shouldFastBreakBuild()) {
      log.lifecycle("\n  -~-  \u001B[1;31mCHECKS FAILED\u001B[0m  -~-\n");
      nomore = true;
      throw new GradleException("Check failed!");
    }
  }

  private boolean printCommandResult(Command cmd, boolean includeDiagnostic) {
    // debug output for TAs
    log.info("COMMAND: {}", cmd.toString());
    log.info("EXIT VALUE: {}", cmd.exitValue());
    log.info("OUTPUT:");

    // actual output of the command should be parsed and colored, etc
    if (cmd instanceof BasicCommand) {
      String output = parseCommandOutput((BasicCommand) cmd, includeDiagnostic);
      log.lifecycle(output);
    }
    if (cmd.exitValue() != Command.SUCCESS) {
      log.info("Check failed!");
      return true;
    }

    return false;
  }

  /**
   * Output the compiled summary to the project's Logger.
   */
  public void showOutputSummary() {
    // log.lifecycle("\n\n  -~-  \u001B[1;36mBeginning check summary\u001B[1;0m  -~-\n\n");
    int totalChecks = completedCommands.size();
    List<Command> failed = completedCommands.stream()
                               .filter(cmd -> cmd.exitValue() != Command.SUCCESS)
                               .collect(Collectors.toList());
    boolean failedChecks = failed.size() > 0;

    if (failedChecks) {
      log.lifecycle("\n\n\u001B[1;33m-~-  \u001B[1;31mFAILURES  \u001B[1;33m-~-\u001B[0m\n");
      for (int i = 0; i < failed.size(); i++) {
        printCommandResult(failed.get(i), true);
      }
    }

    int passedChecks = completedCommands.size() - failed.size();

    StringUtil.border("Passed " + passedChecks + "/" + totalChecks + " ("
            + (Math.round((passedChecks * 100) / (float) totalChecks)) + "%)"
            + " of checks for " + GatorGradleConfig.get().getAssignmentName() + "!",
        failedChecks ? "\u001B[1;31m" : "\u001B[1;32m",
        failedChecks ? "\u001B[1;35m" : "\u001B[1;32m", log);

    if (failedChecks && GatorGradleConfig.get().shouldBreakBuild()) {
      throw new GradleException(
          StringUtil.color(StringUtil.BAD, "Grading checks failed -- scroll up for failures"));
    }
  }

  private CheckResult parseGatorGraderCommand(GatorGraderCommand cmd, boolean includeDiagnostic) {
    CheckResult result = null;
    String output = cmd.getOutput();
    try {
      result = new CheckResult(output);
    } catch (CheckResult.MalformedJsonException ex) {

      // test if the problem was an unsupported argument
      int index = -1;
      String unrec = "gatorgrader.py: error: unrecognized arguments:";
      if ((index = output.indexOf(unrec)) >= 0) {
        index += unrec.length();
        unrec = output.substring(index).trim();
        result = new CheckResult(
            "Unrecognized GatorGrader check",
            false,
            "The " + unrec + " check is not supported"
        );
      } else {
        // only log unknown error when not requesting diagnostic
        // generally the diagnostic request is a second or third print
        if (!includeDiagnostic) {
          log.error(cmd.toString() + " errored: \'" + ex.getMessage() + "\'");
        }
        result = new CheckResult(
            "Unknown GatorGrader check",
            false,
            "The check failed with an unknown error"
        );
      }
    }
    return result;
  }

  private CheckResult parseCommandLineExecutable(BasicCommand cmd, boolean includeDiagnostic) {
    String output = cmd.getOutput();
    StringBuilder diagnostic = new StringBuilder();
    if (includeDiagnostic && output != null && !output.trim().isEmpty()) {
      Scanner scan = new Scanner(output);
      diagnostic.append(cmd.executable() + " diagnostics:\n");
      while (scan.hasNext()) {
        String line = scan.nextLine().trim();
        if (!line.isEmpty()) {
          diagnostic.append(line).append("\n");
        }
      }
    } else {
      diagnostic.append("No diagnostic available");
    }
    return new CheckResult(
        "The file " + cmd.last() + " passes " + cmd.executable(),
        cmd.exitValue() == Command.SUCCESS,
        diagnostic.toString().trim()
    );
  }

  private CheckResult parsePureCommandOutput(BasicCommand cmd, boolean includeDiagnostic) {
    String output = cmd.getOutput();
    StringBuilder diagnostic = new StringBuilder();
    if (includeDiagnostic && output != null && !output.trim().isEmpty()) {
      Scanner scan = new Scanner(output);
      diagnostic.append(cmd + " printed:\n");
      while (scan.hasNext()) {
        String line = scan.nextLine().trim();
        if (!line.isEmpty()) {
          diagnostic.append(line).append("\n");
        }
      }
    } else {
      diagnostic.append("No diagnostic available");
    }
    return new CheckResult(
        cmd.toString() + " executes",
        cmd.exitValue() == Command.SUCCESS,
        diagnostic.toString().trim()
    );
  }

  private String parseCommandOutput(BasicCommand cmd, boolean includeDiagnostic) {
    CheckResult result = null;
    if (cmd instanceof GatorGraderCommand) {
      result = parseGatorGraderCommand((GatorGraderCommand) cmd, includeDiagnostic);
    } else if (GatorGradleConfig.get().isCommandLineExecutable(cmd.executable())) {
      result = parseCommandLineExecutable(cmd, includeDiagnostic);
    } else {
      result = parsePureCommandOutput(cmd, includeDiagnostic);
    }

    return result.textReport(includeDiagnostic);
  }
}
