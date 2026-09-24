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
import java.util.List;
import org.junit.jupiter.api.Test;
class ExecutionEventBufferTest {
  @Test void assignsMonotonicSequenceAndReplaysAfterLastEventId() { ExecutionEventBuffer buffer = new ExecutionEventBuffer(4); assertEquals(1, buffer.append("state", "running").seq()); assertEquals(2, buffer.append("log", "one").seq()); assertEquals(3, buffer.append("metrics", "two").seq()); assertEquals(List.of(2L, 3L), buffer.replayAfter(1).stream().map(ExecutionEvent::seq).toList()); }
  @Test void evictsOldestEventsAtCapacity() { ExecutionEventBuffer buffer = new ExecutionEventBuffer(2); buffer.append("log", "one"); buffer.append("log", "two"); buffer.append("log", "three"); assertEquals(2, buffer.oldestRetainedSequence()); assertEquals(List.of(2L, 3L), buffer.replayAfter(0).stream().map(ExecutionEvent::seq).toList()); }
}
