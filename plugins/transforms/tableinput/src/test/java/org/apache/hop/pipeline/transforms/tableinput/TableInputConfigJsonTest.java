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
package org.apache.hop.pipeline.transforms.tableinput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.apache.hop.metadata.serializer.json.ConfigJsonSerializer;
import org.junit.jupiter.api.Test;

class TableInputConfigJsonTest {

  @Test
  void roundTripsRealTableInputMetaIncludingGroupedOutputFields() throws Exception {
    TableInputField field = new TableInputField();
    field.setName("customer_id");
    field.setType(2);
    field.setLength(12);
    field.setPrecision(0);

    TableInputMeta meta = new TableInputMeta();
    meta.setConnection("warehouse");
    meta.setSql("select customer_id from customers");
    meta.setRowLimit("100");
    meta.setExecuteEachInputRow(true);
    meta.setVariableReplacementActive(true);
    meta.setSqlFromFile("${PROJECT_HOME}/sql/customers.sql");
    meta.setUseNamedParameters(true);
    meta.setSpecifyFields(true);
    meta.setValidateSpecifiedFields(true);
    meta.setFields(List.of(field));

    ObjectNode json = ConfigJsonSerializer.toJson(meta);

    assertEquals("warehouse", json.get("connection").asText());
    assertEquals("select customer_id from customers", json.get("sql").asText());
    assertEquals("customer_id", json.get("fields").get("field").get(0).get("name").asText());
    assertFalse(json.has("field"));

    TableInputMeta restored = ConfigJsonSerializer.fromJson(json, TableInputMeta.class);

    assertEquals("warehouse", restored.getConnection());
    assertEquals(meta.getSql(), restored.getSql());
    assertEquals("100", restored.getRowLimit());
    assertTrue(restored.isExecuteEachInputRow());
    assertTrue(restored.isVariableReplacementActive());
    assertEquals(meta.getSqlFromFile(), restored.getSqlFromFile());
    assertTrue(restored.isUseNamedParameters());
    assertTrue(restored.isSpecifyFields());
    assertTrue(restored.isValidateSpecifiedFields());
    assertEquals(1, restored.getFields().size());
    assertEquals("customer_id", restored.getFields().get(0).getName());
    assertEquals(2, restored.getFields().get(0).getType());
    assertEquals(12, restored.getFields().get(0).getLength());
  }
}
