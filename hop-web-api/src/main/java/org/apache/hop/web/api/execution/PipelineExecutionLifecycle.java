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

import org.apache.hop.core.exception.HopException;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.engine.IPipelineEngine;

/** Bridges a Hop pipeline engine into the web execution event lifecycle. */
public final class PipelineExecutionLifecycle {
  private PipelineExecutionLifecycle() {}

  public static <T extends PipelineMeta> void start(
      String id, ExecutionRegistry<IPipelineEngine<T>> registry) throws HopException {
    ExecutionRegistry.Entry<IPipelineEngine<T>> entry =
        registry.find(id).orElseThrow(() -> new IllegalArgumentException("unknown execution: " + id));
    IPipelineEngine<T> engine = entry.execution();
    engine.addExecutionFinishedListener(
        finished -> {
          entry.events().append(
              "finished", new FinishedEvent(finished.getErrors(), finished.isStopped()));
          registry.markCompleted(id);
        });
    entry.events().append("state", "preparing");
    engine.prepareExecution();
    entry.events().append("state", "running");
    engine.startThreads();
  }

  public record FinishedEvent(int errors, boolean stopped) {}
}
