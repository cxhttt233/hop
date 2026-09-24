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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hop.core.encryption.ITwoWayPasswordEncoder;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.metadata.api.HopMetadataObject;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataObjectFactory;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.junit.jupiter.api.Test;

class FactoryConfigJsonSerializerTest {

  @Test
  void roundTripsFactoryBackedObjectAndNestedSensitiveValue() throws Exception {
    IHopMetadataProvider provider = mock(IHopMetadataProvider.class);
    ITwoWayPasswordEncoder encoder = mock(ITwoWayPasswordEncoder.class);
    when(provider.getTwoWayPasswordEncoder()).thenReturn(encoder);
    when(encoder.encode("secret", true)).thenReturn("Encrypted secret");
    when(encoder.decode("Encrypted secret", true)).thenReturn("secret");

    FactoryConfig config = new FactoryConfig();
    SamplePlugin plugin = new SamplePlugin();
    plugin.value = "jdbc:test";
    plugin.password = "secret";
    config.plugin = plugin;

    ObjectNode json = ConfigJsonSerializer.toJson(config, provider);

    assertEquals("jdbc:test", json.at("/plugin/sample/value").asText());
    assertEquals("Encrypted secret", json.at("/plugin/sample/password").asText());

    FactoryConfig restored = ConfigJsonSerializer.fromJson(json, FactoryConfig.class, provider);

    SamplePlugin restoredPlugin = assertInstanceOf(SamplePlugin.class, restored.plugin);
    assertEquals("jdbc:test", restoredPlugin.value);
    assertEquals("secret", restoredPlugin.password);
  }

  static class FactoryConfig {
    @HopMetadataProperty SamplePluginContract plugin;
  }

  @HopMetadataObject(objectFactory = SamplePluginFactory.class)
  interface SamplePluginContract {}

  static class SamplePlugin implements SamplePluginContract {
    @HopMetadataProperty String value;

    @HopMetadataProperty(password = true)
    String password;
  }

  public static class SamplePluginFactory implements IHopMetadataObjectFactory {
    @Override
    public Object createObject(String id, Object parentObject) throws HopException {
      if (!"sample".equals(id)) {
        throw new HopException("Unknown sample plugin id " + id);
      }
      return new SamplePlugin();
    }

    @Override
    public String getObjectId(Object object) throws HopException {
      if (!(object instanceof SamplePlugin)) {
        throw new HopException("Unsupported sample plugin " + object.getClass().getName());
      }
      return "sample";
    }
  }
}
