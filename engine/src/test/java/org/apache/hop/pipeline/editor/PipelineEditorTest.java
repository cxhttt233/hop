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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.apache.hop.core.gui.Point;
import org.apache.hop.core.undo.ChangeAction;
import org.apache.hop.pipeline.PipelineHopMeta;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.junit.jupiter.api.Test;

class PipelineEditorTest {
  @Test
  void addsAndDeletesHopWithUndoRecords() {
    PipelineMeta pipeline = new PipelineMeta();
    TransformMeta first = transform("first", 10, 20);
    TransformMeta second = transform("second", 30, 40);
    pipeline.addTransform(first);
    pipeline.addTransform(second);
    pipeline.clearUndo();
    pipeline.clearChanged();
    PipelineEditor editor = new PipelineEditor(pipeline);

    PipelineHopMeta hop = editor.addHop("first", "second");
    assertNotNull(hop);
    assertEquals(1, pipeline.getPipelineHops().size());
    assertEquals(ChangeAction.ActionType.NewHop, pipeline.previousUndo().getType());

    pipeline.clearUndo();
    assertEquals(true, editor.deleteHop("first", "second"));
    assertEquals(0, pipeline.getPipelineHops().size());
    assertEquals(ChangeAction.ActionType.DeleteHop, pipeline.previousUndo().getType());
  }

  @Test
  void rejectsInvalidOrDuplicateHops() {
    PipelineMeta pipeline = new PipelineMeta();
    pipeline.addTransform(transform("first", 0, 0));
    pipeline.addTransform(transform("second", 10, 10));
    PipelineEditor editor = new PipelineEditor(pipeline);

    assertEquals(null, editor.addHop("missing", "second"));
    assertEquals(null, editor.addHop("first", "first"));
    assertNotNull(editor.addHop("first", "second"));
    assertEquals(null, editor.addHop("first", "second"));
    assertEquals(false, editor.deleteHop("second", "first"));
  }

  @Test
  void movesTransformsAndRecordsSingleUndoAction() {
    PipelineMeta pipeline = new PipelineMeta();
    TransformMeta first = transform("first", 10, 20);
    TransformMeta second = transform("second", 30, 40);
    pipeline.addTransform(first);
    pipeline.addTransform(second);
    pipeline.clearUndo();
    pipeline.clearChanged();

    int moved = new PipelineEditor(pipeline).moveTransforms(List.of("first", "second"), 5, -10);

    assertEquals(2, moved);
    assertEquals(new Point(15, 10), first.getLocation());
    assertEquals(new Point(35, 30), second.getLocation());
    ChangeAction undo = pipeline.previousUndo();
    assertNotNull(undo);
    assertEquals(ChangeAction.ActionType.PositionTransform, undo.getType());
  }

  @Test
  void ignoresUnknownDuplicateAndNoOpTransforms() {
    PipelineMeta pipeline = new PipelineMeta();
    TransformMeta first = transform("first", 0, 0);
    pipeline.addTransform(first);
    pipeline.clearUndo();

    PipelineEditor editor = new PipelineEditor(pipeline);
    assertEquals(0, editor.moveTransforms(List.of("missing"), 1, 1));
    assertEquals(0, editor.moveTransforms(List.of("first"), -5, -5));
    assertEquals(1, editor.moveTransforms(List.of("first", "first"), 3, 4));
    assertEquals(new Point(3, 4), first.getLocation());
  }

  private static TransformMeta transform(String name, int x, int y) {
    TransformMeta transform = new TransformMeta();
    transform.setName(name);
    transform.setLocation(x, y);
    return transform;
  }
}
