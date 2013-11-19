/*
 * Copyright (c) 2013 Spotify AB
 */

package com.spotify.helios.cli;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.spotify.helios.cli.command.ControlCommand;
import com.spotify.helios.cli.command.HostDeleteCommand;
import com.spotify.helios.cli.command.HostJobsCommand;
import com.spotify.helios.cli.command.HostListCommand;
import com.spotify.helios.cli.command.HostRegisterCommand;
import com.spotify.helios.cli.command.JobCreateCommand;
import com.spotify.helios.cli.command.JobDeployCommand;
import com.spotify.helios.cli.command.JobListCommand;
import com.spotify.helios.cli.command.JobRemoveCommand;
import com.spotify.helios.cli.command.JobStopCommand;
import com.spotify.helios.cli.command.JobUndeployCommand;
import com.spotify.helios.cli.command.MasterListCommand;
import com.spotify.helios.common.LoggingConfig;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.spotify.helios.cli.Target.targetsFrom;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static net.sourceforge.argparse4j.impl.Arguments.SUPPRESS;
import static net.sourceforge.argparse4j.impl.Arguments.append;
import static net.sourceforge.argparse4j.impl.Arguments.fileType;
import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;

public class CliParser {

  private final Namespace options;
  private final ControlCommand command;
  private final LoggingConfig loggingConfig;
  private final Subparsers commandParsers;
  private final CliConfig cliConfig;
  private final List<Target> targets;
  private final String username;

  public CliParser(final String... args)
      throws ArgumentParserException, IOException, JSONException {

    final ArgumentParser parser = ArgumentParsers.newArgumentParser("helios")
        .defaultHelp(true)
        .description("Spotify Helios CLI");

    cliConfig = CliConfig.fromUserConfig();

    final GlobalArgs globalArgs = addGlobalArgs(parser, cliConfig);

    commandParsers = parser.addSubparsers().title("commands");

    setupCommands();

    try {
      this.options = parser.parseArgs(args);
      this.command = (ControlCommand) options.get("command");
      final String username = options.getString(globalArgs.usernameArg.getDest());
      this.username = (username == null) ? cliConfig.getUsername() : username;
      this.loggingConfig = new LoggingConfig(options.getInt(globalArgs.verbose.getDest()),
                                             options.getBoolean(globalArgs.syslog.getDest()),
                                             (File) options.get(globalArgs.logconfig.getDest()),
                                             options.getBoolean(globalArgs.noLogSetup.getDest()));
    } catch (ArgumentParserException e) {
      parser.handleError(e);
      throw e;
    }

    // Merge sites and explicit endpoints into master endpoints
    final List<String> explicitEndpoints = options.getList(globalArgs.masterArg.getDest());
    final String sitesArgument = options.getString(globalArgs.sitesArg.getDest());
    final String srvName = options.getString(globalArgs.srvNameArg.getDest());
    final List<Target> explicitTargets = targetsFrom(explicitEndpoints);
    final String sitesString = sitesArgument == null ? cliConfig.getSitesString() : sitesArgument;
    final Iterable<String> sites;
    if (explicitEndpoints.isEmpty()) {
      sites = filter(asList(sitesString.split(",")), not(equalTo("")));
    } else {
      sites = ImmutableList.of();
    }
    final List<Target> siteTargets = targetsFrom(srvName, sites);
    this.targets = ImmutableList.copyOf(concat(explicitTargets, siteTargets));
  }

  private void setupCommands() {
    // Job commands
    final Subparsers job = p("job").addSubparsers();
    new JobListCommand(p(job, "list"));
    new JobCreateCommand(p(job, "create"));
    new JobDeployCommand(p(job, "deploy"));
    new JobUndeployCommand(p(job, "undeploy"));
    new JobStopCommand(p(job, "stop"));
    new JobRemoveCommand(p(job, "remove"));

    // Host commands
    final Subparsers host = p("host").addSubparsers();
    new HostListCommand(p(host, "list"));
    new HostJobsCommand(p(host, "jobs"));
    new HostRegisterCommand(p(host, "register"));
    new HostDeleteCommand(p(host, "delete"));

    // Master Commands
    final Subparsers master = p("master").addSubparsers();
    new MasterListCommand(p(master, "list"));
  }

  public List<Target> getTargets() {
    return targets;
  }

  public String getUsername() {
    return username;
  }

  private static class GlobalArgs {

    private final Argument masterArg;
    private final Argument sitesArg;
    private final Argument srvNameArg;
    private final Argument usernameArg;
    private final Argument verbose;
    private final Argument syslog;
    private final Argument logconfig;
    private final Argument noLogSetup;

    GlobalArgs(final ArgumentParser parser, final CliConfig cliConfig) {
      final ArgumentGroup globalArgs = parser.addArgumentGroup("global options");

      masterArg = parser.addArgument("-z", "--master")
          .action(append())
          .setDefault(Lists.newArrayList())
          .help("master endpoint");

      sitesArg = parser.addArgument("-s", "--sites")
          .help(format("sites (default: %s)", cliConfig.getSitesString()));

      srvNameArg = parser.addArgument("--srv-name")
          .setDefault(cliConfig.getSrvName())
          .help("master srv name");

      usernameArg = parser.addArgument("--username")
          .setDefault(System.getProperty("user.name"))
          .help("username");

      verbose = globalArgs.addArgument("-v", "--verbose")
          .action(Arguments.count());

      syslog = globalArgs.addArgument("--syslog")
          .help("Log to syslog.")
          .action(storeTrue());

      logconfig = globalArgs.addArgument("--logconfig")
          .type(fileType().verifyExists().verifyCanRead())
          .help("Logback configuration file.");

      noLogSetup = globalArgs.addArgument("--no-log-setup")
          .action(storeTrue())
          .help(SUPPRESS);
    }
  }

  private GlobalArgs addGlobalArgs(final ArgumentParser parser, final CliConfig cliConfig) {
    return new GlobalArgs(parser, cliConfig);
  }

  public Namespace getNamespace() {
    return options;
  }

  public ControlCommand getCommand() {
    return command;
  }

  public LoggingConfig getLoggingConfig() {
    return loggingConfig;
  }

  private Subparser p(final String name) {
    return p(commandParsers, name);
  }

  private Subparser p(final Subparsers subparsers, final String name) {
    final Subparser subparser = subparsers.addParser(name, true);
    addGlobalArgs(subparser, cliConfig);
    return subparser;
  }
}