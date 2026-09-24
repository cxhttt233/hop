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
import java.lang.reflect.Proxy;import java.time.Duration;
import org.apache.hop.core.logging.*;import org.apache.hop.pipeline.PipelineMeta;import org.apache.hop.pipeline.engine.IPipelineEngine;import org.junit.jupiter.api.Test;
class ExecutionLogPublisherTest {
 @Test @SuppressWarnings("unchecked") void publishesOnlyExecutionLogChannel(){HopLogStore.init();IPipelineEngine<PipelineMeta> engine=(IPipelineEngine<PipelineMeta>)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{IPipelineEngine.class},(proxy,method,args)->{if(method.getName().equals("getLogChannelId"))return "run-channel";return defaultValue(method.getReturnType());});ExecutionRegistry<IPipelineEngine<PipelineMeta>> registry=new ExecutionRegistry<>(Duration.ofHours(1),8);var entry=registry.register("run-1","alice",engine);try(var publisher=new ExecutionLogPublisher<>(entry)){publisher.start();HopLogStore.getAppender().addLogggingEvent(event("accepted","run-channel",10));HopLogStore.getAppender().addLogggingEvent(event("ignored","other-channel",11));}var events=entry.events().replayAfter(0);assertEquals(1,events.size());assertEquals("log",events.getFirst().type());var log=(ExecutionLogPublisher.LogEvent)events.getFirst().payload();assertEquals(10,log.timestamp());assertEquals("accepted",log.message());}
 private static HopLoggingEvent event(String text,String channel,long timestamp){return new HopLoggingEvent(new LogMessage(text,channel,LogLevel.BASIC),timestamp,LogLevel.BASIC);}private static Object defaultValue(Class<?> type){if(!type.isPrimitive())return null;if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;return 0;}
}
