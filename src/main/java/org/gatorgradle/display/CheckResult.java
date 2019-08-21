package org.gatorgradle.display;

import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gatorgradle.util.Console;
import org.gatorgradle.util.StringUtil;

public class CheckResult {
  public static class MalformedJsonException extends Exception {
    public MalformedJsonException(String json) {
      super("Failed to parse json:\n---\n" + json + "\n---");
    }

    public MalformedJsonException(String reason, String json) {
      super("Failed to parse json -- " + reason
          + (json != null ? ":\n---\n" + json + "\n---" : ""));
    }
  }

  public static final Pattern CHECK_REGEX =
      Pattern.compile(".*\"check\":\\s*\"(.*?)\".*", Pattern.DOTALL);
  public static final Pattern OUTCOME_REGEX =
      Pattern.compile(".*\"outcome\":\\s*([Tt]rue|[Ff]alse).*", Pattern.DOTALL);
  public static final Pattern DIAGNOSTIC_REGEX =
      Pattern.compile(".*\"diagnostic\":\\s*\"(.*?)\".*", Pattern.DOTALL);

  public String check;
  public Boolean outcome;
  public String diagnostic;

  /**
   * Construct a CheckResult.
   *
   * @param check      the check completed
   * @param outcome    the outcome of the check
   * @param diagnostic the diagnostic output from the check
   **/
  public CheckResult(String check, Boolean outcome, String diagnostic) {
    this.check = check;
    this.outcome = outcome;
    this.diagnostic = diagnostic;
  }

  /**
   * Parses the given JSON string, setting check, outcome,
   * and diagnostic member variables when found accordingly.
   *
   * <p>This method assumes the json passed represents only one
   * result, and any duplicates will overwrite values.
   *
   * @param json the json to parse
   * @throws MalformedJsonException if the json given is not valid for a CheckResult
   *
   */
  public CheckResult(String json) throws MalformedJsonException {
    if (json == null) {
      throw new MalformedJsonException("Null JSON text", null);
    }
    // handle escaped quotes by replacing them with '
    json = json.replaceAll("\\\\\"", "'").trim();

    Matcher matcher = CHECK_REGEX.matcher(json);
    if (!matcher.matches()) {
      throw new MalformedJsonException("Could not find 'check' key in", json);
    }
    this.check = matcher.group(1);


    matcher.usePattern(OUTCOME_REGEX);
    matcher.reset();
    if (!matcher.matches()) {
      throw new MalformedJsonException("Could not find 'outcome' key in", json);
    }
    this.outcome = Boolean.parseBoolean(matcher.group(1));


    matcher.usePattern(DIAGNOSTIC_REGEX);
    matcher.reset();
    if (!matcher.matches()) {
      throw new MalformedJsonException("Could not find 'diagnostic' key in", json);
    }
    this.diagnostic = matcher.group(1);
  }

  public static final String PASS_SYMBOL =
      StringUtil.color(StringUtil.GOOD, "\u2714"); // heavy check (✔)
  public static final String FAIL_SYMBOL =
      StringUtil.color(StringUtil.BAD, "\u2718"); // heavy cross (✘)
  public static final String FIX_SYMBOL =
      StringUtil.color("\u001B[1;33m", "\u2794"); // right arrow (➔)

  /**
   * Returns a string representation of this result.
   *
   * @param includeDiagnostic include diagnostic?
   * @return a string
   **/
  public String textReport(boolean includeDiagnostic) {
    if (outcome) {
      return PASS_SYMBOL + "  " + check;
    } else {
      String output = FAIL_SYMBOL + "  " + check;
      if (includeDiagnostic) {
        output += "\n   " + FIX_SYMBOL + "  " + StringUtil.color(StringUtil.FIX, diagnostic);
      }
      return output;
    }
  }
}