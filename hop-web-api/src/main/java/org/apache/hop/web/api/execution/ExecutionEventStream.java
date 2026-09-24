/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hop.web.api.execution;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Transport-neutral view of an execution event stream.
 *
 * <p>The Jersey SSE resource remains responsible for HTTP status mapping, SSE framing and heartbeat
 * scheduling. This class owns only the execution lookup/ownership/replay boundary so that transport
 * adapters do not duplicate registry state.
 */
public final class ExecutionEventStream<T> {
  private final ExecutionRegistry<T> registry;

  public ExecutionEventStream(ExecutionRegistry<T> registry) {
    this.registry = registry;
  }

  /** Returns retained events strictly newer than the supplied SSE Last-Event-ID. */
  public List<ExecutionEvent> replay(String executionId, String owner, long lastEventId) {
    ExecutionRegistry.Entry<T> entry =
        registry
            .find(executionId)
            .orElseThrow(() -> new NoSuchElementException("unknown execution: " + executionId));
    if (!entry.owner().equals(owner)) {
      throw new SecurityException("execution is not owned by the current session");
    }
    return entry.events().replayAfter(lastEventId);
  }
}
