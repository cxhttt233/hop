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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.engine.IEngineComponent;
import org.apache.hop.pipeline.engine.IPipelineEngine;

/** Publishes the execution metrics snapshot required by the web execution UI once per second. */
public final class ExecutionMetricsPublisher<T extends PipelineMeta> implements AutoCloseable {
  private final ExecutionRegistry.Entry<IPipelineEngine<T>> entry;
  private final ScheduledExecutorService scheduler;
  public ExecutionMetricsPublisher(ExecutionRegistry.Entry<IPipelineEngine<T>> entry) {
    this.entry = entry;
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread thread = new Thread(r, "hop-web-execution-metrics"); thread.setDaemon(true); return thread; });
  }
  public void start() { scheduler.scheduleAtFixedRate(this::publish, 0, 1, TimeUnit.SECONDS); }
  void publish() {
    try {
      entry.execution().getEngineMetrics();
      List<ComponentMetrics> components = entry.execution().getComponents().stream().map(ExecutionMetricsPublisher::snapshot).toList();
      entry.events().append("metrics", components);
    } catch (RuntimeException ignored) { }
  }
  private static ComponentMetrics snapshot(IEngineComponent component) {
    return new ComponentMetrics(component.getName(), component.getCopyNr(), component.getLinesRead(), component.getLinesWritten(), component.getLinesInput(), component.getLinesOutput(), component.getLinesRejected(), component.getLinesUpdated(), component.getErrors(), component.getStatusDescription(), component.getExecutionDuration(), component.getInputBufferSize(), component.getOutputBufferSize());
  }
  @Override public void close() { scheduler.shutdownNow(); }
  public record ComponentMetrics(String name, int copy, long linesRead, long linesWritten, long linesInput, long linesOutput, long linesRejected, long linesUpdated, long errors, String status, long duration, long inputBufferSize, long outputBufferSize) {}
}
