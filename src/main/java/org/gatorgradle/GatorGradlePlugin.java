package org.gatorgradle;

import java.io.File;
import java.util.Locale;

import org.gatorgradle.config.GatorGradleConfig;
import org.gatorgradle.task.GatorGradleTask;
import org.gatorgradle.util.Console;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

/**
 * GatorGradlePlugin applies the plugin to a project, registers
 * the grade task, and sets up some sensible defaults.
 * TODO: allow DSL configuration block to specify
 *        CONFIG_FILE_LOCATION and GATORGRADER_HOME.
 */
public class GatorGradlePlugin implements Plugin<Project> {
  public static final String WINDOWS = "windows";
  public static final String LINUX = "linux";
  public static final String MACOS = "mac";

  public static final String GATORGRADER_HOME;
  public static final String CONFIG_FILE_LOCATION;
  public static final String USER_HOME;
  public static final String F_SEP;
  public static final String OS;

  static {
    F_SEP = File.separator;
    USER_HOME = System.getProperty("user.home");

    String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    if (os.contains("linux")) {
      OS = LINUX;
    } else if (os.contains("windows")) {
      OS = WINDOWS;
    } else if (os.contains("mac")) {
      OS = MACOS;
    } else {
      OS = "unsupported";
    }

    // TODO: is this a sensible default for gg home? - probably only on linux and mac
    if (OS.equals(LINUX) || OS.equals(MACOS)) {
      GATORGRADER_HOME = USER_HOME + F_SEP + ".local" + F_SEP + "share" + F_SEP + "gatorgrader";
    } else {
      GATORGRADER_HOME = USER_HOME + F_SEP + ".gatorgrader";
    }

    CONFIG_FILE_LOCATION = "config" + F_SEP + "gatorgrader.yml";
  }

  /**
   * Applies the GatorGrader plugin to the given project.
   *
   * @param project the project to apply GatorGrader to
   */
  public void apply(final Project project) {
    Logger logger = project.getLogger();
    // set config file location, then generate config
    // TODO: what should we do for config file location?
    GatorGradleConfig config =
        GatorGradleConfig.create(project.file(CONFIG_FILE_LOCATION).toPath());

    // ensure we got a configuration
    if (config == null) {
      throw new GradleException(
          "GatorGradle grade task's configuration was not specified correctly!");
    }

    logger.lifecycle("Configured GatorGradle {}",
        GatorGradlePlugin.class.getPackage().getImplementationVersion());

    // create gatorgradle 'grade' task
    project.getTasks().create("grade", GatorGradleTask.class, task -> {
      // default grade task uses config from above and project dir as grade
      task.setConfig(config);
      task.setWorkingDir(project.getProjectDir());
    });
  }
}
