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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hop.pipeline.IExecutionFinishedListener;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.engine.IPipelineEngine;
import org.junit.jupiter.api.Test;

class PipelineExecutionLifecycleTest {
  @Test
  @SuppressWarnings("unchecked")
  void startsEngineAndMarksExecutionCompletedWhenFinished() throws Exception {
    List<String> calls = new ArrayList<>();
    AtomicReference<IExecutionFinishedListener<IPipelineEngine<PipelineMeta>>> finishedListener =
        new AtomicReference<>();
    IPipelineEngine<PipelineMeta> engine =
        (IPipelineEngine<PipelineMeta>)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {IPipelineEngine.class},
                (proxy, method, args) -> {
                  switch (method.getName()) {
                    case "addExecutionFinishedListener" -> {
                      finishedListener.set(
                          (IExecutionFinishedListener<IPipelineEngine<PipelineMeta>>) args[0]);
                      calls.add("listener");
                    }
                    case "prepareExecution" -> calls.add("prepare");
                    case "startThreads" -> calls.add("start");
                    case "getErrors" -> { return 0; }
                    case "isStopped" -> { return false; }
                    default -> { return defaultValue(method.getReturnType()); }
                  }
                  return null;
                });

    ExecutionRegistry<IPipelineEngine<PipelineMeta>> registry =
        new ExecutionRegistry<>(Duration.ofHours(1), 8);
    ExecutionRegistry.Entry<IPipelineEngine<PipelineMeta>> entry =
        registry.register("run-1", "alice", engine);

    PipelineExecutionLifecycle.start("run-1", registry);

    assertEquals(List.of("listener", "prepare", "start"), calls);
    assertEquals(
        List.of("state", "state"), entry.events().replayAfter(0).stream().map(ExecutionEvent::type).toList());
    finishedListener.get().finished(engine);
    assertTrue(entry.completedAt().isPresent());
    assertEquals("finished", entry.events().replayAfter(2).getFirst().type());
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) return null;
    if (type == boolean.class) return false;
    if (type == byte.class) return (byte) 0;
    if (type == short.class) return (short) 0;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == float.class) return 0F;
    if (type == double.class) return 0D;
    if (type == char.class) return '\0';
    return null;
  }
}
