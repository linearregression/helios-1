/**
 * Copyright (C) 2014 Spotify AB
 */

package com.spotify.helios.system;

import com.google.common.collect.ImmutableMap;

import com.spotify.helios.agent.docker.DefaultDockerClient;
import com.spotify.helios.agent.docker.DockerClient;
import com.spotify.helios.agent.docker.LogStream;
import com.spotify.helios.common.descriptors.JobId;
import com.spotify.helios.common.descriptors.TaskStatus;

import org.junit.Test;

import java.util.List;

import static com.spotify.helios.agent.docker.DockerClient.LogsParameter.STDERR;
import static com.spotify.helios.agent.docker.DockerClient.LogsParameter.STDOUT;
import static com.spotify.helios.common.descriptors.HostStatus.Status.UP;
import static com.spotify.helios.common.descriptors.TaskStatus.State.EXITED;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.Assert.assertEquals;

public class SyslogRedirectionTest extends SystemTestBase {

  @Test
  public void test() throws Exception {
    // While this test doesn't specifically test that the output actually goes to syslog, it tests
    // just about every other part of it, and specifically, that the output doesn't get to
    // docker, and that the redirector executable exists and doesn't do anything terribly stupid.
    startDefaultMaster();
    startDefaultAgent(getTestHost(), "--syslog-redirect", "10.0.3.1:6514");
    awaitHostStatus(getTestHost(), UP, LONG_WAIT_MINUTES, MINUTES);

    final DockerClient dockerClient = new DefaultDockerClient(DOCKER_ENDPOINT);

    final List<String> command = asList("sh", "-c", "echo should-be-redirected");

    // Create job
    final JobId jobId = createJob(JOB_NAME, JOB_VERSION, "ubuntu:12.04", command,
                                  ImmutableMap.of("FOO", "4711",
                                                  "BAR", "deadbeef"));

    // deploy
    deployJob(jobId, getTestHost());

    final TaskStatus taskStatus = awaitTaskState(jobId, getTestHost(), EXITED);

    final String log;
    try (LogStream logs = dockerClient.logs(taskStatus.getContainerId(), STDOUT, STDERR)) {
      log = logs.readFully();
    }

    // should be nothing in the docker output log, either error text or our message
    assertEquals("", log);
  }

}
