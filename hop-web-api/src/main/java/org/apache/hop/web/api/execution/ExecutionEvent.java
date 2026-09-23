/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.hop.web.api.execution;

import java.time.Instant;

/** An ordered event emitted by one web execution. */
public record ExecutionEvent(long seq, String type, Object payload, Instant createdAt) {}
