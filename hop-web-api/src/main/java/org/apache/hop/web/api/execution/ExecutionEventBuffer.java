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

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Thread-safe bounded replay buffer used by an execution SSE stream. */
public final class ExecutionEventBuffer {
  private final int capacity;
  private final Clock clock;
  private final Deque<ExecutionEvent> events;
  private long sequence;

  public ExecutionEventBuffer(int capacity) {
    this(capacity, Clock.systemUTC());
  }

  ExecutionEventBuffer(int capacity, Clock clock) {
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be positive");
    }
    this.capacity = capacity;
    this.clock = clock;
    this.events = new ArrayDeque<>(capacity);
  }

  public synchronized ExecutionEvent append(String type, Object payload) {
    ExecutionEvent event = new ExecutionEvent(++sequence, type, payload, clock.instant());
    if (events.size() == capacity) {
      events.removeFirst();
    }
    events.addLast(event);
    return event;
  }

  /** Returns retained events strictly newer than the client's Last-Event-ID. */
  public synchronized List<ExecutionEvent> replayAfter(long lastEventId) {
    List<ExecutionEvent> replay = new ArrayList<>();
    for (ExecutionEvent event : events) {
      if (event.seq() > lastEventId) {
        replay.add(event);
      }
    }
    return List.copyOf(replay);
  }

  public synchronized long latestSequence() {
    return sequence;
  }

  public synchronized long oldestRetainedSequence() {
    return events.isEmpty() ? sequence + 1 : events.getFirst().seq();
  }
}
