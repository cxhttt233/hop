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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class ExecutionEventStreamTest {
  @Test
  void replaysStrictlyAfterLastEventId() {
    ExecutionRegistry<Object> registry = new ExecutionRegistry<>(Duration.ofHours(24), 8);
    ExecutionRegistry.Entry<Object> entry = registry.register("exec-1", "owner-1", new Object());
    entry.events().append("state", "preparing");
    ExecutionEvent running = entry.events().append("state", "running");
    ExecutionEvent metrics = entry.events().append("metrics", "snapshot");

    List<ExecutionEvent> replay =
        new ExecutionEventStream<>(registry).replay("exec-1", "owner-1", running.seq());

    assertEquals(List.of(metrics), replay);
  }

  @Test
  void rejectsUnknownExecution() {
    ExecutionRegistry<Object> registry = new ExecutionRegistry<>(Duration.ofHours(24), 8);

    assertThrows(
        NoSuchElementException.class,
        () -> new ExecutionEventStream<>(registry).replay("missing", "owner-1", 0));
  }

  @Test
  void rejectsExecutionOwnedByAnotherSession() {
    ExecutionRegistry<Object> registry = new ExecutionRegistry<>(Duration.ofHours(24), 8);
    registry.register("exec-1", "owner-1", new Object());

    assertThrows(
        SecurityException.class,
        () -> new ExecutionEventStream<>(registry).replay("exec-1", "owner-2", 0));
  }

  @Test
  void completedExecutionRemainsReplayableUntilRegistryCleanup() {
    ExecutionRegistry<Object> registry = new ExecutionRegistry<>(Duration.ofHours(24), 8);
    ExecutionRegistry.Entry<Object> entry = registry.register("exec-1", "owner-1", new Object());
    ExecutionEvent finished = entry.events().append("finished", "done");
    registry.markCompleted("exec-1");

    List<ExecutionEvent> replay =
        new ExecutionEventStream<>(registry).replay("exec-1", "owner-1", 0);

    assertEquals(List.of(finished), replay);
  }
}
