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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.metadata.api.HopMetadataObject;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IEnumHasCode;
import org.apache.hop.metadata.api.IHopMetadata;
import org.apache.hop.metadata.api.IHopMetadataObjectFactory;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.metadata.api.IHopMetadataSerializer;
import org.apache.hop.metadata.util.ReflectionUtil;

/**
 * JSON transport codec for Transform/Action configuration objects.
 *
 * <p>This is deliberately not a persistence serializer: .hpl/.hwf files continue to use the
 * existing XML path. Only fields carrying {@link HopMetadataProperty} participate, so transport
 * keys stay aligned with Hop metadata/XML keys.
 */
public final class ConfigJsonSerializer {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ConfigJsonSerializer() {}

  public static ObjectNode toJson(Object config) throws HopException {
    return toJson(config, null);
  }

  public static ObjectNode toJson(Object config, IHopMetadataProvider metadataProvider)
      throws HopException {
    if (config == null) {
      return MAPPER.createObjectNode();
    }
    return writeObject(config, metadataProvider);
  }

  public static <T> T fromJson(JsonNode json, Class<T> type) throws HopException {
    return fromJson(json, type, null);
  }

  public static <T> T fromJson(
      JsonNode json, Class<T> type, IHopMetadataProvider metadataProvider) throws HopException {
    try {
      T value = type.getDeclaredConstructor().newInstance();
      readObject(json, value, metadataProvider);
      return value;
    } catch (HopException e) {
      throw e;
    } catch (Exception e) {
      throw new HopException("Unable to create config object " + type.getName(), e);
    }
  }

  private static ObjectNode writeObject(Object value, IHopMetadataProvider metadataProvider)
      throws HopException {
    ObjectNode result = MAPPER.createObjectNode();
    for (Field field : ReflectionUtil.findAllFields(value.getClass())) {
      HopMetadataProperty property = field.getAnnotation(HopMetadataProperty.class);
      if (property == null || property.isExcludedFromSerialization()) {
        continue;
      }
      String key = key(property, field);
      try {
        Object fieldValue = ReflectionUtil.getFieldValue(value, field.getName(), isBoolean(field));
        ObjectNode target = group(result, property, true);
        target.set(key, writeValue(fieldValue, field, property, metadataProvider));
      } catch (Exception e) {
        throw new HopException("Unable to serialize config property '" + key + "'", e);
      }
    }
    return result;
  }

  private static JsonNode writeValue(
      Object value,
      Field field,
      HopMetadataProperty property,
      IHopMetadataProvider metadataProvider)
      throws HopException {
    if (value == null) {
      return MAPPER.nullNode();
    }
    if (value instanceof List<?> list) {
      ArrayNode array = MAPPER.createArrayNode();
      Class<?> itemType = listItemType(field);
      for (Object item : list) {
        if (item == null) {
          array.addNull();
        } else if (property.storeWithName()) {
          array.add(ReflectionUtil.getObjectName(item));
        } else if (item instanceof String || item instanceof Number || item instanceof Boolean) {
          array.addPOJO(item);
        } else if (item instanceof Enum<?> enumItem) {
          array.add(enumItem.name());
        } else {
          array.add(writePojo(item, itemType, metadataProvider));
        }
      }
      return array;
    }
    if (value instanceof Map<?, ?> map) {
      Class<?>[] types = mapTypes(field);
      if (!String.class.equals(types[0]) || !String.class.equals(types[1])) {
        throw new HopException(
            "Config map property '" + field.getName() + "' only supports Map<String, String>");
      }
      ObjectNode object = MAPPER.createObjectNode();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (entry.getKey() != null) {
          object.put((String) entry.getKey(), (String) entry.getValue());
        }
      }
      return object;
    }
    if (property.storeWithName()) {
      return MAPPER.valueToTree(ReflectionUtil.getObjectName(value));
    }
    if (value instanceof Enum<?> enumValue) {
      if (property.storeWithCode() && value instanceof IEnumHasCode coded) {
        return MAPPER.valueToTree(coded.getCode());
      }
      return MAPPER.valueToTree(enumValue.name());
    }
    if (value instanceof String stringValue && property.password()) {
      return MAPPER.valueToTree(
          requireProvider(metadataProvider, field)
              .getTwoWayPasswordEncoder()
              .encode(stringValue, true));
    }
    if (value instanceof String || value instanceof Number || value instanceof Boolean) {
      return MAPPER.valueToTree(value);
    }
    return writePojo(value, field.getType(), metadataProvider);
  }

  private static ObjectNode writePojo(
      Object value, Class<?> declaredType, IHopMetadataProvider metadataProvider)
      throws HopException {
    ObjectNode properties = writeObject(value, metadataProvider);
    HopMetadataObject metadataObject = declaredType.getAnnotation(HopMetadataObject.class);
    if (metadataObject == null) {
      return properties;
    }
    try {
      IHopMetadataObjectFactory factory =
          metadataObject.objectFactory().getDeclaredConstructor().newInstance();
      String id = factory.getObjectId(value);
      ObjectNode wrapper = MAPPER.createObjectNode();
      wrapper.set(id, properties);
      return wrapper;
    } catch (Exception e) {
      throw new HopException(
          "Unable to serialize factory-backed config object " + declaredType.getName(), e);
    }
  }

  private static void readObject(
      JsonNode json, Object target, IHopMetadataProvider metadataProvider) throws HopException {
    if (json == null || !json.isObject()) {
      throw new HopException("Config JSON must be an object");
    }
    for (Field field : ReflectionUtil.findAllFields(target.getClass())) {
      HopMetadataProperty property = field.getAnnotation(HopMetadataProperty.class);
      if (property == null || property.isExcludedFromSerialization()) {
        continue;
      }
      String key = key(property, field);
      JsonNode container = group(json, property);
      JsonNode node = container == null ? null : container.get(key);
      if (node == null) {
        if (isBoolean(field)) {
          set(target, field, property.defaultBoolean());
        }
        continue;
      }
      set(target, field, readValue(node, field, property, metadataProvider));
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Object readValue(
      JsonNode node,
      Field field,
      HopMetadataProperty property,
      IHopMetadataProvider metadataProvider)
      throws HopException {
    if (node.isNull()) {
      return null;
    }
    Class<?> type = field.getType();
    if (property.storeWithName() && !List.class.equals(type)) {
      return loadNamedReference(node.asText(), type, metadataProvider, field);
    }
    if (String.class.equals(type)) {
      String value = node.asText();
      if (property.password()) {
        value =
            requireProvider(metadataProvider, field)
                .getTwoWayPasswordEncoder()
                .decode(value, true);
      }
      return value;
    }
    if (int.class.equals(type) || Integer.class.equals(type)) {
      return node.asInt();
    }
    if (long.class.equals(type) || Long.class.equals(type)) {
      return node.asLong();
    }
    if (boolean.class.equals(type) || Boolean.class.equals(type)) {
      return node.asBoolean();
    }
    if (double.class.equals(type) || Double.class.equals(type)) {
      return node.asDouble();
    }
    if (type.isEnum()) {
      if (property.storeWithCode()) {
        for (Object constant : type.getEnumConstants()) {
          if (constant instanceof IEnumHasCode coded && node.asText().equals(coded.getCode())) {
            return constant;
          }
        }
        if (StringUtils.isNotEmpty(property.enumNameWhenNotFound())) {
          return Enum.valueOf((Class<? extends Enum>) type, property.enumNameWhenNotFound());
        }
        return null;
      }
      return Enum.valueOf((Class<? extends Enum>) type, node.asText());
    }
    if (List.class.equals(type)) {
      Class<?> itemType = listItemType(field);
      java.util.ArrayList<Object> values = new java.util.ArrayList<>();
      for (JsonNode item : node) {
        if (property.storeWithName()) {
          values.add(loadNamedReference(item.asText(), itemType, metadataProvider, field));
        } else if (String.class.equals(itemType)) {
          values.add(item.asText());
        } else if (Integer.class.equals(itemType)) {
          values.add(item.asInt());
        } else {
          values.add(readPojo(item, itemType, metadataProvider));
        }
      }
      return values;
    }
    if (Map.class.equals(type)) {
      Class<?>[] types = mapTypes(field);
      if (!String.class.equals(types[0]) || !String.class.equals(types[1])) {
        throw new HopException(
            "Config map property '" + field.getName() + "' only supports Map<String, String>");
      }
      java.util.HashMap<String, String> values = new java.util.HashMap<>();
      node.fields().forEachRemaining(entry -> values.put(entry.getKey(), entry.getValue().asText()));
      return values;
    }
    return readPojo(node, type, metadataProvider);
  }

  private static Object readPojo(
      JsonNode node, Class<?> declaredType, IHopMetadataProvider metadataProvider)
      throws HopException {
    try {
      HopMetadataObject metadataObject = declaredType.getAnnotation(HopMetadataObject.class);
      Object child;
      JsonNode properties = node;
      if (metadataObject == null) {
        child = declaredType.getDeclaredConstructor().newInstance();
      } else {
        if (!node.isObject() || node.size() != 1) {
          throw new HopException(
              "Factory-backed config property "
                  + declaredType.getName()
                  + " must contain one object id");
        }
        Map.Entry<String, JsonNode> entry = node.fields().next();
        IHopMetadataObjectFactory factory =
            metadataObject.objectFactory().getDeclaredConstructor().newInstance();
        child = factory.createObject(entry.getKey(), null);
        properties = entry.getValue();
      }
      readObject(properties, child, metadataProvider);
      return child;
    } catch (HopException e) {
      throw e;
    } catch (Exception e) {
      throw new HopException("Unable to create nested config property " + declaredType.getName(), e);
    }
  }

  private static Class<?> listItemType(Field field) throws HopException {
    if (!(field.getGenericType() instanceof ParameterizedType parameterizedType)) {
      throw new HopException("Config list property '" + field.getName() + "' has no item type");
    }
    if (!(parameterizedType.getActualTypeArguments()[0] instanceof Class<?> itemType)) {
      throw new HopException(
          "Config list property '" + field.getName() + "' has unsupported item type");
    }
    return itemType;
  }

  private static Class<?>[] mapTypes(Field field) throws HopException {
    if (!(field.getGenericType() instanceof ParameterizedType parameterizedType)
        || parameterizedType.getActualTypeArguments().length != 2
        || !(parameterizedType.getActualTypeArguments()[0] instanceof Class<?> keyType)
        || !(parameterizedType.getActualTypeArguments()[1] instanceof Class<?> valueType)) {
      throw new HopException("Config map property '" + field.getName() + "' has unsupported types");
    }
    return new Class<?>[] {keyType, valueType};
  }

  @SuppressWarnings("unchecked")
  private static Object loadNamedReference(
      String name, Class<?> type, IHopMetadataProvider metadataProvider, Field field)
      throws HopException {
    IHopMetadataProvider provider = requireProvider(metadataProvider, field);
    if (!IHopMetadata.class.isAssignableFrom(type)) {
      throw new HopException(
          "Config property '"
              + field.getName()
              + "' uses storeWithName but type does not implement IHopMetadata: "
              + type.getName());
    }
    IHopMetadataSerializer<?> serializer =
        provider.getSerializer((Class<? extends IHopMetadata>) type);
    return serializer.load(name);
  }

  private static IHopMetadataProvider requireProvider(
      IHopMetadataProvider metadataProvider, Field field) throws HopException {
    if (metadataProvider == null) {
      throw new HopException(
          "Config property '"
              + field.getName()
              + "' requires an IHopMetadataProvider for transport semantics");
    }
    return metadataProvider;
  }

  private static ObjectNode group(ObjectNode root, HopMetadataProperty property, boolean create) {
    if (StringUtils.isEmpty(property.groupKey())) {
      return root;
    }
    JsonNode existing = root.get(property.groupKey());
    if (existing instanceof ObjectNode objectNode) {
      return objectNode;
    }
    if (!create) {
      return null;
    }
    ObjectNode objectNode = MAPPER.createObjectNode();
    root.set(property.groupKey(), objectNode);
    return objectNode;
  }

  private static JsonNode group(JsonNode root, HopMetadataProperty property) {
    return StringUtils.isEmpty(property.groupKey()) ? root : root.get(property.groupKey());
  }

  private static void set(Object target, Field field, Object value) throws HopException {
    try {
      ReflectionUtil.setFieldValue(target, field.getName(), field.getType(), value);
    } catch (Exception e) {
      throw new HopException("Unable to set config property '" + field.getName() + "'", e);
    }
  }

  private static boolean isBoolean(Field field) {
    return boolean.class.equals(field.getType()) || Boolean.class.equals(field.getType());
  }

  private static String key(HopMetadataProperty property, Field field) {
    return StringUtils.isNotEmpty(property.key()) ? property.key() : field.getName();
  }
}
