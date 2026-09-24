/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hop.web.api.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WebSessionRegistryTest {
  @Test
  void isolatesStateAndReleasesItWhenSessionEnds() {
    AtomicInteger closed = new AtomicInteger();
    WebSessionRegistry<TestSession> registry =
        new WebSessionRegistry<>(id -> new TestSession(id, closed));

    TestSession first = registry.getOrCreate("first");
    assertSame(first, registry.getOrCreate("first"));
    assertNotSame(first, registry.getOrCreate("second"));
    assertEquals(2, registry.size());

    registry.remove("first");
    assertNull(registry.get("first"));
    assertEquals(1, closed.get());

    registry.close();
    assertEquals(0, registry.size());
    assertEquals(2, closed.get());
  }

  private record TestSession(String id, AtomicInteger closed) implements AutoCloseable {
    @Override
    public void close() {
      closed.incrementAndGet();
    }
  }
}
