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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.encryption.ITwoWayPasswordEncoder;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IEnumHasCode;
import org.apache.hop.metadata.api.IHopMetadata;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.metadata.api.IHopMetadataSerializer;
import org.junit.jupiter.api.Test;

class ConfigJsonSerializerTest {

  @Test
  void roundTripsAnnotatedConfigUsingMetadataKeys() throws Exception {
    SampleConfig config = new SampleConfig();
    config.name = "input.csv";
    config.limit = 25;
    config.enabled = true;
    config.mode = Mode.FAST;
    config.items.add(new Item("first", 1));
    config.ignored = "desktop-only";

    ObjectNode json = ConfigJsonSerializer.toJson(config);

    assertEquals("input.csv", json.get("file_name").asText());
    assertEquals("F", json.get("mode").asText());
    assertFalse(json.has("ignored"));

    SampleConfig restored = ConfigJsonSerializer.fromJson(json, SampleConfig.class);
    assertEquals(config.name, restored.name);
    assertEquals(config.limit, restored.limit);
    assertEquals(config.enabled, restored.enabled);
    assertEquals(config.mode, restored.mode);
    assertEquals(1, restored.items.size());
    assertEquals("first", restored.items.get(0).name);
    assertEquals(1, restored.items.get(0).ordinal);
    assertNull(restored.ignored);
  }

  @Test
  void roundTripsGroupedListUsingMetadataGroupKey() throws Exception {
    GroupedConfig config = new GroupedConfig();
    config.fields.add(new Item("first", 1));
    config.fields.add(new Item("second", 2));

    ObjectNode json = ConfigJsonSerializer.toJson(config);

    assertEquals("first", json.get("fields").get("field").get(0).get("name").asText());
    assertFalse(json.has("field"));

    GroupedConfig restored = ConfigJsonSerializer.fromJson(json, GroupedConfig.class);
    assertEquals(2, restored.fields.size());
    assertEquals("second", restored.fields.get(1).name);
    assertEquals(2, restored.fields.get(1).ordinal);
  }

  @Test
  void missingBooleanUsesMetadataDefault() throws Exception {
    ObjectNode json = ConfigJsonSerializer.toJson(new SampleConfig());
    json.remove("enabled");

    SampleConfig restored = ConfigJsonSerializer.fromJson(json, SampleConfig.class);

    assertEquals(true, restored.enabled);
  }

  @Test
  void roundTripsPasswordUsingProviderEncoder() throws Exception {
    IHopMetadataProvider provider = mock(IHopMetadataProvider.class);
    ITwoWayPasswordEncoder encoder = mock(ITwoWayPasswordEncoder.class);
    when(provider.getTwoWayPasswordEncoder()).thenReturn(encoder);
    when(encoder.encode("secret", true)).thenReturn("Encrypted secret");
    when(encoder.decode("Encrypted secret", true)).thenReturn("secret");

    SensitiveConfig config = new SensitiveConfig();
    config.password = "secret";

    ObjectNode json = ConfigJsonSerializer.toJson(config, provider);
    SensitiveConfig restored = ConfigJsonSerializer.fromJson(json, SensitiveConfig.class, provider);

    assertEquals("Encrypted secret", json.get("password").asText());
    assertEquals("secret", restored.password);
  }

  @Test
  void resolvesNamedMetadataReferenceUsingProviderSerializer() throws Exception {
    IHopMetadataProvider provider = mock(IHopMetadataProvider.class);
    @SuppressWarnings("unchecked")
    IHopMetadataSerializer<NamedMetadata> serializer = mock(IHopMetadataSerializer.class);
    NamedMetadata referenced = new NamedMetadata("connection-a");
    when(provider.getSerializer(NamedMetadata.class)).thenReturn(serializer);
    when(serializer.load("connection-a")).thenReturn(referenced);

    NamedReferenceConfig config = new NamedReferenceConfig();
    config.reference = referenced;

    ObjectNode json = ConfigJsonSerializer.toJson(config, provider);
    NamedReferenceConfig restored =
        ConfigJsonSerializer.fromJson(json, NamedReferenceConfig.class, provider);

    assertEquals("connection-a", json.get("reference").asText());
    assertEquals(referenced, restored.reference);
  }

  static class SampleConfig {
    @HopMetadataProperty(key = "file_name")
    String name;

    @HopMetadataProperty int limit;
    @HopMetadataProperty(defaultBoolean = true)
    boolean enabled;
    @HopMetadataProperty(storeWithCode = true)
    Mode mode;
    @HopMetadataProperty List<Item> items = new ArrayList<>();
    @HopMetadataProperty(isExcludedFromSerialization = true)
    String ignored;

    SampleConfig() {}
  }

  static class GroupedConfig {
    @HopMetadataProperty(key = "field", groupKey = "fields")
    List<Item> fields = new ArrayList<>();
  }

  static class SensitiveConfig {
    @HopMetadataProperty(password = true)
    String password;
  }

  static class NamedReferenceConfig {
    @HopMetadataProperty(storeWithName = true)
    NamedMetadata reference;
  }

  static class NamedMetadata implements IHopMetadata {
    private String name;
    private String metadataProviderName;
    private String virtualPath;

    NamedMetadata() {}

    NamedMetadata(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    public String getMetadataProviderName() {
      return metadataProviderName;
    }

    @Override
    public void setMetadataProviderName(String metadataProviderName) {
      this.metadataProviderName = metadataProviderName;
    }

    @Override
    public String getVirtualPath() {
      return virtualPath;
    }

    @Override
    public void setVirtualPath(String virtualPath) {
      this.virtualPath = virtualPath;
    }

    @Override
    public String getFullName() {
      return name;
    }
  }

  static class Item {
    @HopMetadataProperty String name;
    @HopMetadataProperty int ordinal;

    Item() {}

    Item(String name, int ordinal) {
      this.name = name;
      this.ordinal = ordinal;
    }
  }

  enum Mode implements IEnumHasCode {
    FAST("F"),
    SAFE("S");

    private final String code;

    Mode(String code) {
      this.code = code;
    }

    @Override
    public String getCode() {
      return code;
    }
  }
}
