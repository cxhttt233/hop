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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ExecutionRegistryTest {
  @Test
  void registersAndExplicitlyRemovesExecution() {
    ExecutionRegistry<Object> registry = new ExecutionRegistry<>(Duration.ofHours(24), 8);
    Object engine = new Object();
    ExecutionRegistry.Entry<Object> entry = registry.register("e1", "alice", engine);
    assertEquals("alice", entry.owner());
    assertSame(engine, registry.find("e1").orElseThrow().execution());
    assertSame(entry, registry.remove("e1").orElseThrow());
    assertTrue(registry.find("e1").isEmpty());
  }

  @Test
  void rejectsDuplicateIds() {
    ExecutionRegistry<Object> registry = new ExecutionRegistry<>(Duration.ofHours(24), 8);
    registry.register("e1", "alice", new Object());
    assertThrows(
        IllegalArgumentException.class, () -> registry.register("e1", "bob", new Object()));
  }

  @Test
  void cleansCompletedButNotRunningExecutions() {
    ExecutionRegistry<Object> registry = new ExecutionRegistry<>(Duration.ZERO, 8);
    registry.register("done", "alice", new Object());
    registry.register("running", "alice", new Object());
    registry.markCompleted("done");
    assertEquals(1, registry.cleanupExpired());
    assertTrue(registry.find("done").isEmpty());
    assertTrue(registry.find("running").isPresent());
  }

  @Test
  void eachExecutionOwnsAnIndependentReplaySequence() {
    ExecutionRegistry<Object> registry = new ExecutionRegistry<>(Duration.ofHours(24), 2);
    var first = registry.register("e1", "alice", new Object());
    var second = registry.register("e2", "alice", new Object());
    assertEquals(1, first.events().append("state", "running").seq());
    assertEquals(1, second.events().append("state", "running").seq());
  }
}
