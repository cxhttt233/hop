/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hop.metadata.serializer.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hop.core.HopClientEnvironment;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.database.DatabasePluginType;
import org.apache.hop.core.encryption.ITwoWayPasswordEncoder;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.util.TestUtil;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.junit.rules.RestoreHopEnvironmentExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RestoreHopEnvironmentExtension.class)
class DatabaseMetaConfigJsonSerializerTest {

  @BeforeAll
  static void setUpOnce() throws HopException {
    DatabasePluginType.getInstance().searchPlugins();
    HopClientEnvironment.init();
    TestUtil.registerTestPluginTypes();
  }

  @Test
  void roundTripsRealDatabaseMetaIncludingPasswordAndAttributes() throws Exception {
    IHopMetadataProvider provider = mock(IHopMetadataProvider.class);
    ITwoWayPasswordEncoder encoder = mock(ITwoWayPasswordEncoder.class);
    when(provider.getTwoWayPasswordEncoder()).thenReturn(encoder);
    when(encoder.encode("secret", true)).thenReturn("Encrypted secret");
    when(encoder.decode("Encrypted secret", true)).thenReturn("secret");

    DatabaseMeta database =
        new DatabaseMeta(
            "orders", "H2", "Native", "db.internal", "orders", "9092", "hop", "secret");
    database.getIDatabase().getAttributes().put("CUSTOM_OPTION", "enabled");

    ObjectNode json = ConfigJsonSerializer.toJson(database, provider);
    DatabaseMeta restored = ConfigJsonSerializer.fromJson(json, DatabaseMeta.class, provider);

    assertEquals("orders", restored.getName());
    assertEquals("H2", restored.getPluginId());
    assertEquals("db.internal", restored.getHostname());
    assertEquals("orders", restored.getDatabaseName());
    assertEquals("9092", restored.getPort());
    assertEquals("hop", restored.getUsername());
    assertEquals("secret", restored.getPassword());
    assertEquals("enabled", restored.getIDatabase().getAttributes().get("CUSTOM_OPTION"));
    assertEquals("Encrypted secret", json.get("rdbms").elements().next().get("password").asText());
  }
}
