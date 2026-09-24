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
package org.apache.hop.pipeline.transforms.sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.apache.hop.metadata.serializer.json.ConfigJsonSerializer;
import org.junit.jupiter.api.Test;

class SortRowsConfigJsonTest {

  @Test
  void roundTripsRealSortRowsMetaIncludingGroupedFields() throws Exception {
    SortRowsField field = new SortRowsField("customer_id", false, false, true, 2, true);

    SortRowsMeta meta = new SortRowsMeta();
    meta.setSortFields(List.of(field));
    meta.setDirectory("${java.io.tmpdir}/hop-sort");
    meta.setPrefix("customers");
    meta.setSortSize("250000");
    meta.setFreeMemoryLimit("20");
    meta.setOnlyPassingUniqueRows(true);
    meta.setCompressFiles(true);
    meta.setCompressFilesVariable("${SORT_COMPRESS}");

    ObjectNode json = ConfigJsonSerializer.toJson(meta);

    assertEquals("customer_id", json.get("fields").get("field").get(0).get("name").asText());
    assertFalse(json.has("field"));
    assertEquals("${java.io.tmpdir}/hop-sort", json.get("directory").asText());
    assertTrue(json.get("unique_rows").asBoolean());

    SortRowsMeta restored = ConfigJsonSerializer.fromJson(json, SortRowsMeta.class);

    assertEquals(meta.getDirectory(), restored.getDirectory());
    assertEquals("customers", restored.getPrefix());
    assertEquals("250000", restored.getSortSize());
    assertEquals("20", restored.getFreeMemoryLimit());
    assertTrue(restored.isOnlyPassingUniqueRows());
    assertTrue(restored.isCompressFiles());
    assertEquals("${SORT_COMPRESS}", restored.getCompressFilesVariable());
    assertEquals(1, restored.getSortFields().size());
    SortRowsField restoredField = restored.getSortFields().get(0);
    assertEquals("customer_id", restoredField.getFieldName());
    assertFalse(restoredField.isAscending());
    assertFalse(restoredField.isCaseSensitive());
    assertTrue(restoredField.isCollatorEnabled());
    assertEquals(2, restoredField.getCollatorStrength());
    assertTrue(restoredField.isPreSortedField());
  }
}
