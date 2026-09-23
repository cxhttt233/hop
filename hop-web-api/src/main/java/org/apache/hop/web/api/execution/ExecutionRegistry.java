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
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Owns web execution lifecycle independently from the HTTP session lifetime. */
public final class ExecutionRegistry<T> {
  private final Map<String, Entry<T>> executions = new ConcurrentHashMap<>();
  private final Duration completedTtl;
  private final int eventCapacity;
  private final Clock clock;

  public ExecutionRegistry(Duration completedTtl, int eventCapacity) {
    this(completedTtl, eventCapacity, Clock.systemUTC());
  }

  ExecutionRegistry(Duration completedTtl, int eventCapacity, Clock clock) {
    if (completedTtl.isNegative()) {
      throw new IllegalArgumentException("completedTtl must not be negative");
    }
    this.completedTtl = completedTtl;
    this.eventCapacity = eventCapacity;
    this.clock = clock;
  }

  public Entry<T> register(String id, String owner, T execution) {
    Entry<T> entry =
        new Entry<>(owner, execution, new ExecutionEventBuffer(eventCapacity), clock.instant());
    if (executions.putIfAbsent(id, entry) != null) {
      throw new IllegalArgumentException("execution already registered: " + id);
    }
    return entry;
  }

  public Optional<Entry<T>> find(String id) {
    return Optional.ofNullable(executions.get(id));
  }

  public Optional<Entry<T>> remove(String id) {
    return Optional.ofNullable(executions.remove(id));
  }

  public void markCompleted(String id) {
    require(id).completedAt = clock.instant();
  }

  public int cleanupExpired() {
    Instant cutoff = clock.instant().minus(completedTtl);
    int before = executions.size();
    executions.entrySet().removeIf(e -> e.getValue().isCompletedBefore(cutoff));
    return before - executions.size();
  }

  private Entry<T> require(String id) {
    Entry<T> entry = executions.get(id);
    if (entry == null) throw new IllegalArgumentException("unknown execution: " + id);
    return entry;
  }

  public static final class Entry<T> {
    private final String owner;
    private final T execution;
    private final ExecutionEventBuffer events;
    private final Instant createdAt;
    private volatile Instant completedAt;

    private Entry(String owner, T execution, ExecutionEventBuffer events, Instant createdAt) {
      this.owner = owner;
      this.execution = execution;
      this.events = events;
      this.createdAt = createdAt;
    }

    public String owner() {
      return owner;
    }

    public T execution() {
      return execution;
    }

    public ExecutionEventBuffer events() {
      return events;
    }

    public Instant createdAt() {
      return createdAt;
    }

    public Optional<Instant> completedAt() {
      return Optional.ofNullable(completedAt);
    }

    private boolean isCompletedBefore(Instant cutoff) {
      Instant completed = completedAt;
      return completed != null && !completed.isAfter(cutoff);
    }
  }
}
