/*
 * Copyright (c) 2014 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.helios.servicescommon.statistics;

public interface SupervisorMetrics {

  void supervisorStarted();

  void supervisorStopped();

  void supervisorClosed();

  void containersRunning();

  void containersExited();

  void containersThrewException();

  void containerStarted();

  MetricsContext containerPull();

  void imageCacheHit();

  void imageCacheMiss();

  void dockerTimeout();

  void supervisorRun();

  MeterRates getDockerTimeoutRates();
  MeterRates getContainersThrewExceptionRates();
  MeterRates getSupervisorRunRates();

}
