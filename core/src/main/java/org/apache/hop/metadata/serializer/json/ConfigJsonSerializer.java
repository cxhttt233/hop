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
import org.apache.commons.lang3.StringUtils;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IEnumHasCode;
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
    if (config == null) {
      return MAPPER.createObjectNode();
    }
    return writeObject(config);
  }

  public static <T> T fromJson(JsonNode json, Class<T> type) throws HopException {
    try {
      T value = type.getDeclaredConstructor().newInstance();
      readObject(json, value);
      return value;
    } catch (HopException e) {
      throw e;
    } catch (Exception e) {
      throw new HopException("Unable to create config object " + type.getName(), e);
    }
  }

  private static ObjectNode writeObject(Object value) throws HopException {
    ObjectNode result = MAPPER.createObjectNode();
    for (Field field : ReflectionUtil.findAllFields(value.getClass())) {
      HopMetadataProperty property = field.getAnnotation(HopMetadataProperty.class);
      if (property == null || property.isExcludedFromSerialization()) {
        continue;
      }
      String key = key(property, field);
      try {
        Object fieldValue = ReflectionUtil.getFieldValue(value, field.getName(), isBoolean(field));
        result.set(key, writeValue(fieldValue, field, property));
      } catch (Exception e) {
        throw new HopException("Unable to serialize config property '" + key + "'", e);
      }
    }
    return result;
  }

  private static JsonNode writeValue(Object value, Field field, HopMetadataProperty property)
      throws HopException {
    if (value == null) {
      return MAPPER.nullNode();
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
    if (value instanceof String || value instanceof Number || value instanceof Boolean) {
      return MAPPER.valueToTree(value);
    }
    if (value instanceof List<?> list) {
      ArrayNode array = MAPPER.createArrayNode();
      for (Object item : list) {
        if (item == null) {
          array.addNull();
        } else if (item instanceof String || item instanceof Number || item instanceof Boolean) {
          array.addPOJO(item);
        } else if (item instanceof Enum<?> enumItem) {
          array.add(enumItem.name());
        } else {
          array.add(writeObject(item));
        }
      }
      return array;
    }
    return writeObject(value);
  }

  private static void readObject(JsonNode json, Object target) throws HopException {
    if (json == null || !json.isObject()) {
      throw new HopException("Config JSON must be an object");
    }
    for (Field field : ReflectionUtil.findAllFields(target.getClass())) {
      HopMetadataProperty property = field.getAnnotation(HopMetadataProperty.class);
      if (property == null || property.isExcludedFromSerialization()) {
        continue;
      }
      String key = key(property, field);
      JsonNode node = json.get(key);
      if (node == null) {
        if (isBoolean(field)) {
          set(target, field, property.defaultBoolean());
        }
        continue;
      }
      if (property.storeWithName()) {
        throw new HopException("Config property '" + key + "' is a named metadata reference");
      }
      set(target, field, readValue(node, field, property));
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Object readValue(JsonNode node, Field field, HopMetadataProperty property)
      throws HopException {
    if (node.isNull()) {
      return null;
    }
    Class<?> type = field.getType();
    if (String.class.equals(type)) {
      return node.asText();
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
      if (!(field.getGenericType() instanceof ParameterizedType parameterizedType)) {
        throw new HopException(
            "Config list property '" + key(property, field) + "' has no item type");
      }
      Class<?> itemType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
      java.util.ArrayList<Object> values = new java.util.ArrayList<>();
      for (JsonNode item : node) {
        if (String.class.equals(itemType)) {
          values.add(item.asText());
        } else if (Integer.class.equals(itemType)) {
          values.add(item.asInt());
        } else {
          Object child;
          try {
            child = itemType.getDeclaredConstructor().newInstance();
          } catch (Exception e) {
            throw new HopException("Unable to create config list item " + itemType.getName(), e);
          }
          readObject(item, child);
          values.add(child);
        }
      }
      return values;
    }
    try {
      Object child = type.getDeclaredConstructor().newInstance();
      readObject(node, child);
      return child;
    } catch (HopException e) {
      throw e;
    } catch (Exception e) {
      throw new HopException("Unable to create nested config property " + type.getName(), e);
    }
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
