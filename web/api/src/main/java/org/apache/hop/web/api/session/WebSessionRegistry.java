/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hop.web.api.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Owns the server-side state associated with HTTP sessions. */
public class WebSessionRegistry<T extends AutoCloseable> implements AutoCloseable {
  private final Map<String, T> sessions = new ConcurrentHashMap<>();
  private final Function<String, T> factory;

  public WebSessionRegistry(Function<String, T> factory) {
    this.factory = factory;
  }

  public T getOrCreate(String sessionId) {
    return sessions.computeIfAbsent(sessionId, factory);
  }

  public T get(String sessionId) {
    return sessions.get(sessionId);
  }

  public void remove(String sessionId) {
    close(sessions.remove(sessionId));
  }

  public int size() {
    return sessions.size();
  }

  @Override
  public void close() {
    sessions.values().forEach(WebSessionRegistry::close);
    sessions.clear();
  }

  private static void close(AutoCloseable session) {
    if (session == null) {
      return;
    }
    try {
      session.close();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to release web session resources", e);
    }
  }
}
