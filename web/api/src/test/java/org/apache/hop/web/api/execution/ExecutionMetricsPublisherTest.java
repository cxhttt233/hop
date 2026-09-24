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
import java.lang.reflect.Proxy;import java.time.Duration;import java.util.List;import org.apache.hop.pipeline.PipelineMeta;import org.apache.hop.pipeline.engine.*;import org.junit.jupiter.api.Test;
class ExecutionMetricsPublisherTest {
 @Test @SuppressWarnings("unchecked") void publishesComponentSnapshot(){IEngineComponent component=component();IPipelineEngine<PipelineMeta> engine=(IPipelineEngine<PipelineMeta>)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{IPipelineEngine.class},(proxy,method,args)->{if(method.getName().equals("getComponents"))return List.of(component);return defaultValue(method.getReturnType());});ExecutionRegistry<IPipelineEngine<PipelineMeta>> registry=new ExecutionRegistry<>(Duration.ofHours(1),8);var entry=registry.register("run-1","alice",engine);try(var publisher=new ExecutionMetricsPublisher<>(entry)){publisher.publish();}ExecutionEvent event=entry.events().replayAfter(0).getFirst();assertEquals("metrics",event.type());var metrics=(ExecutionMetricsPublisher.ComponentMetrics)((List<?>)event.payload()).getFirst();assertEquals("input",metrics.name());assertEquals(7,metrics.linesRead());assertEquals(2,metrics.errors());assertEquals("Running",metrics.status());}
 private static IEngineComponent component(){return(IEngineComponent)Proxy.newProxyInstance(ExecutionMetricsPublisherTest.class.getClassLoader(),new Class<?>[]{IEngineComponent.class},(proxy,method,args)->switch(method.getName()){case"getName"->"input";case"getCopyNr"->1;case"getLinesRead"->7L;case"getErrors"->2L;case"getStatusDescription"->"Running";case"getExecutionDuration"->42L;default->defaultValue(method.getReturnType());});}private static Object defaultValue(Class<?> type){if(!type.isPrimitive())return null;if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;return 0;}
}
