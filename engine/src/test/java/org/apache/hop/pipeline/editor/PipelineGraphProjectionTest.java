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
package org.apache.hop.pipeline.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.hop.core.NotePadMeta;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.core.xml.XmlHandler;
import org.apache.hop.metadata.serializer.memory.MemoryMetadataProvider;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.junit.jupiter.api.Test;

class PipelineGraphProjectionTest {
  @Test
  void projectsCommandMutationsAndRemainsStableAfterReload() throws Exception {
    PipelineMeta pipeline = new PipelineMeta();
    pipeline.addTransform(transform("first", "MockFirst", 10, 20));
    pipeline.addTransform(transform("second", "MockSecond", 30, 40));
    pipeline.clearUndo();
    pipeline.clearChanged();

    PipelineEditor editor = new PipelineEditor(pipeline);
    assertEquals(1, editor.moveTransforms(List.of("first"), 25, 15));
    editor.addHop("first", "second");
    editor.setHopEnabled("first", "second", false);

    PipelineGraphProjection.Graph expected =
        new PipelineGraphProjection.Graph(
            List.of(
                new PipelineGraphProjection.Node(
                    "first", "first", "transform", "MockFirst", "transform", 35, 35),
                new PipelineGraphProjection.Node(
                    "second", "second", "transform", "MockSecond", "transform", 30, 40)),
            List.of(new PipelineGraphProjection.Edge("first->second", "first", "second", false)));
    assertEquals(expected, PipelineGraphProjection.project(pipeline));
    assertEquals(expected, PipelineGraphProjection.project(reload(pipeline)));
  }

  @Test
  void preservesAddDeleteAndUndoAcrossReload() throws Exception {
    PipelineMeta pipeline = new PipelineMeta();
    pipeline.addTransform(transform("first", "MockFirst", 10, 20));
    pipeline.clearUndo();
    pipeline.clearChanged();

    PipelineEditor editor = new PipelineEditor(pipeline);
    assertTrue(editor.addTransform(transform("second", "MockSecond", 30, 40)));
    editor.addHop("first", "second");
    PipelineGraphProjection.Graph connected = PipelineGraphProjection.project(pipeline);

    assertTrue(editor.deleteTransform("second"));
    assertTrue(editor.undo());
    assertEquals(connected, PipelineGraphProjection.project(pipeline));
    assertEquals(connected, PipelineGraphProjection.project(reload(pipeline)));
  }

  @Test
  void projectsNotesAndPreservesThemAcrossReload() throws Exception {
    PipelineMeta pipeline = new PipelineMeta();
    pipeline.getNotes().add(new NotePadMeta("Remember this", 120, 80, 240, 100));

    PipelineGraphProjection.Graph expected =
        new PipelineGraphProjection.Graph(
            List.of(
                new PipelineGraphProjection.Node(
                    "note:0", "Remember this", "note", null, null, 120, 80)),
            List.of());

    assertEquals(expected, PipelineGraphProjection.project(pipeline));
    assertEquals(expected, PipelineGraphProjection.project(reload(pipeline)));
  }

  private static PipelineMeta reload(PipelineMeta pipeline) throws Exception {
    Variables variables = new Variables();
    String xml = pipeline.getXml(variables);
    return new PipelineMeta(
        XmlHandler.loadXmlString(xml, PipelineMeta.XML_TAG), new MemoryMetadataProvider());
  }

  private static TransformMeta transform(String name, String pluginId, int x, int y) {
    TransformMeta transform = new TransformMeta();
    transform.setName(name);
    transform.setTransformPluginId(pluginId);
    transform.setLocation(x, y);
    return transform;
  }
}
