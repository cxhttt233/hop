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
package org.apache.hop.pipeline.transforms.selectvalues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.apache.hop.metadata.serializer.json.ConfigJsonSerializer;
import org.junit.jupiter.api.Test;

class SelectValuesConfigJsonTest {

  @Test
  void roundTripsRealSelectValuesMetaIncludingNestedOptions() throws Exception {
    SelectField selected = new SelectField();
    selected.setName("customer_id");
    selected.setRename("id");
    selected.setLength(12);
    selected.setPrecision(0);

    DeleteField removed = new DeleteField();
    removed.setName("internal_note");

    SelectMetadataChange metadataChange = new SelectMetadataChange();
    metadataChange.setName("amount");
    metadataChange.setRename("amount_decimal");
    metadataChange.setType("BigNumber");
    metadataChange.setLength(18);
    metadataChange.setPrecision(4);
    metadataChange.setConversionMask("#,##0.0000");
    metadataChange.setDecimalSymbol(".");
    metadataChange.setGroupingSymbol(",");

    SelectOptions options = new SelectOptions();
    options.setSelectFields(List.of(selected));
    options.setSelectingAndSortingUnspecifiedFields(true);
    options.setDeleteName(List.of(removed));
    options.setMeta(List.of(metadataChange));

    SelectValuesMeta meta = new SelectValuesMeta();
    meta.setSelectOption(options);

    ObjectNode json = ConfigJsonSerializer.toJson(meta);

    assertEquals("customer_id", json.get("fields").get("field").get(0).get("name").asText());
    assertEquals("id", json.get("fields").get("field").get(0).get("rename").asText());
    assertEquals("internal_note", json.get("fields").get("remove").get(0).get("name").asText());
    assertEquals("amount", json.get("fields").get("meta").get(0).get("name").asText());
    assertEquals("BigNumber", json.get("fields").get("meta").get(0).get("type").asText());
    assertTrue(json.get("fields").get("select_unspecified").asBoolean());

    SelectValuesMeta restored = ConfigJsonSerializer.fromJson(json, SelectValuesMeta.class);

    assertTrue(restored.getSelectOption().isSelectingAndSortingUnspecifiedFields());
    assertEquals(1, restored.getSelectOption().getSelectFields().size());
    SelectField restoredField = restored.getSelectOption().getSelectFields().get(0);
    assertEquals("customer_id", restoredField.getName());
    assertEquals("id", restoredField.getRename());
    assertEquals(12, restoredField.getLength());
    assertEquals(0, restoredField.getPrecision());
    assertEquals(1, restored.getSelectOption().getDeleteName().size());
    assertEquals("internal_note", restored.getSelectOption().getDeleteName().get(0).getName());
    assertEquals(1, restored.getSelectOption().getMeta().size());
    SelectMetadataChange restoredMetadata = restored.getSelectOption().getMeta().get(0);
    assertEquals("amount", restoredMetadata.getName());
    assertEquals("amount_decimal", restoredMetadata.getRename());
    assertEquals("BigNumber", restoredMetadata.getType());
    assertEquals(18, restoredMetadata.getLength());
    assertEquals(4, restoredMetadata.getPrecision());
    assertEquals("#,##0.0000", restoredMetadata.getConversionMask());
    assertEquals(".", restoredMetadata.getDecimalSymbol());
    assertEquals(",", restoredMetadata.getGroupingSymbol());
  }
}
