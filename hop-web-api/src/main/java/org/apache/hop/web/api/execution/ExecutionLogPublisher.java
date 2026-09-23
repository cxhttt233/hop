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
import org.apache.hop.core.logging.HopLogStore;
import org.apache.hop.core.logging.HopLoggingEvent;
import org.apache.hop.core.logging.IHopLoggingEventListener;
import org.apache.hop.core.logging.LogMessage;
import org.apache.hop.core.logging.LoggingRegistry;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.engine.IPipelineEngine;

/** Bridges Hop logging events for one execution into its bounded SSE replay buffer. */
public final class ExecutionLogPublisher<T extends PipelineMeta> implements AutoCloseable {
  private final ExecutionRegistry.Entry<IPipelineEngine<T>> entry;
  private final String rootLogChannelId;
  private final IHopLoggingEventListener listener;

  public ExecutionLogPublisher(ExecutionRegistry.Entry<IPipelineEngine<T>> entry) {
    this.entry = entry;
    this.rootLogChannelId = entry.execution().getLogChannelId();
    this.listener = this::onEvent;
  }

  public void start() {
    HopLogStore.getAppender().addLoggingEventListener(listener);
  }

  private void onEvent(HopLoggingEvent event) {
    if (!(event.getMessage() instanceof LogMessage message) || !belongsToExecution(message)) {
      return;
    }
    entry.events().append("log", new LogEvent(event.timeStamp, event.getLevel().getCode(),
        message.getSubject(), message.getMessage()));
  }

  private boolean belongsToExecution(LogMessage message) {
    List<String> channels = LoggingRegistry.getInstance().getLogChannelChildren(rootLogChannelId);
    return channels != null && channels.contains(message.getLogChannelId());
  }

  @Override
  public void close() {
    HopLogStore.getAppender().removeLoggingEventListener(listener);
  }

  public record LogEvent(long timestamp, String level, String subject, String message) {}
}
