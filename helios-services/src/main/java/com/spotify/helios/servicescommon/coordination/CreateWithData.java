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

package com.spotify.helios.servicescommon.coordination;

import org.apache.curator.framework.api.transaction.CuratorTransaction;

import java.util.Arrays;

class CreateWithData implements ZooKeeperOperation {

  private final String path;
  private final byte[] data;

  CreateWithData(final String path, final byte[] data) {
    this.path = path;
    this.data = data;
  }

  @Override
  public void register(final CuratorTransaction transaction) throws Exception {
    transaction.create().forPath(path, data);
  }

  @Override
  public String toString() {
    return "CreateWithData{" +
           "path='" + path + '\'' +
           '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final CreateWithData that = (CreateWithData) o;

    if (path != null ? !path.equals(that.path) : that.path != null) {
      return false;
    }
    return Arrays.equals(data, that.data);

  }

  @Override
  public int hashCode() {
    int result = path != null ? path.hashCode() : 0;
    result = 31 * result + Arrays.hashCode(data);
    return result;
  }
}
