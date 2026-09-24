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
  void addsTransformAndRoundTripsUndoRedo() {
    PipelineMeta pipeline = pipelineWithTwoTransforms();
    PipelineEditor editor = new PipelineEditor(pipeline);
    TransformMeta third = transform("third", 50, 60);

    assertEquals(true, editor.addTransform(third));
    assertEquals(third, pipeline.findTransform("third"));
    assertEquals(ChangeAction.ActionType.NewTransform, pipeline.viewPreviousUndo().getType());
    assertEquals(true, editor.undo());
    assertEquals(null, pipeline.findTransform("third"));
    assertEquals(true, editor.redo());
    assertEquals(third, pipeline.findTransform("third"));
    assertEquals(false, editor.addTransform(transform("third", 70, 80)));
  }

  @Test
  void addsAndDeletesHopWithUndoRecords() {
    PipelineMeta pipeline = pipelineWithTwoTransforms();
    PipelineEditor editor = new PipelineEditor(pipeline);

    PipelineHopMeta hop = editor.addHop("first", "second");
    assertNotNull(hop);
    assertEquals(1, pipeline.getPipelineHops().size());
    assertEquals(ChangeAction.ActionType.NewPipelineHop, pipeline.previousUndo().getType());

    pipeline.clearUndo();
    assertEquals(true, editor.deleteHop("first", "second"));
    assertEquals(0, pipeline.getPipelineHops().size());
    assertEquals(ChangeAction.ActionType.DeletePipelineHop, pipeline.previousUndo().getType());
  }

  @Test
  void deletesTransformAndAttachedHopAsLinkedUndoActions() {
    PipelineMeta pipeline = pipelineWithTwoTransforms();
    PipelineEditor editor = new PipelineEditor(pipeline);
    assertNotNull(editor.addHop("first", "second"));
    pipeline.clearUndo();

    assertEquals(true, editor.deleteTransform("first"));
    assertEquals(null, pipeline.findTransform("first"));
    assertEquals(0, pipeline.nrPipelineHops());
    ChangeAction deleteTransform = pipeline.previousUndo();
    assertNotNull(deleteTransform);
    assertEquals(ChangeAction.ActionType.DeleteTransform, deleteTransform.getType());
    assertEquals(ChangeAction.ActionType.DeletePipelineHop, pipeline.viewPreviousUndo().getType());
    assertEquals(true, pipeline.viewPreviousUndo().getNextAlso());
  }

  @Test
  void rejectsUnknownTransformDeleteWithoutUndo() {
    PipelineMeta pipeline = pipelineWithTwoTransforms();
    pipeline.clearUndo();

    assertEquals(false, new PipelineEditor(pipeline).deleteTransform("missing"));
    assertEquals(2, pipeline.nrTransforms());
    assertEquals(null, pipeline.previousUndo());
  }

  @Test
  void enablesDisablesAndFlipsHopWithUndoRecords() {
    PipelineMeta pipeline = pipelineWithTwoTransforms();
    PipelineEditor editor = new PipelineEditor(pipeline);
    PipelineHopMeta hop = editor.addHop("first", "second");
    pipeline.clearUndo();

    assertEquals(true, editor.setHopEnabled("first", "second", false));
    assertEquals(false, hop.isEnabled());
    assertEquals(ChangeAction.ActionType.ChangePipelineHop, pipeline.previousUndo().getType());

    pipeline.clearUndo();
    assertEquals(true, editor.flipHop("first", "second"));
    assertEquals("second", hop.getFromTransform().getName());
    assertEquals("first", hop.getToTransform().getName());
    assertEquals(ChangeAction.ActionType.ChangePipelineHop, pipeline.previousUndo().getType());
  }

  @Test
  void rejectsNoOpOrConflictingHopMutations() {
    PipelineMeta pipeline = pipelineWithTwoTransforms();
    PipelineEditor editor = new PipelineEditor(pipeline);
    PipelineHopMeta forward = editor.addHop("first", "second");
    assertNotNull(forward);

    pipeline.clearUndo();
    assertEquals(false, editor.setHopEnabled("missing", "second", false));
    assertEquals(false, editor.setHopEnabled("first", "second", true));
    assertEquals(false, editor.flipHop("missing", "second"));

    forward.setEnabled(false);
    assertNotNull(editor.addHop("second", "first"));
    pipeline.clearUndo();
    assertEquals(false, editor.flipHop("first", "second"));
    assertEquals(null, pipeline.previousUndo());
  }

  @Test
  void rejectsInvalidOrDuplicateHops() {
    PipelineMeta pipeline = pipelineWithTwoTransforms();
    PipelineEditor editor = new PipelineEditor(pipeline);

    assertEquals(null, editor.addHop("missing", "second"));
    assertEquals(null, editor.addHop("first", "first"));
    assertNotNull(editor.addHop("first", "second"));
    assertEquals(null, editor.addHop("first", "second"));
    assertEquals(false, editor.deleteHop("second", "first"));
  }

  @Test
  void movesTransformsAndRecordsSingleUndoAction() {
    PipelineMeta pipeline = pipelineWithTwoTransforms();
    TransformMeta first = pipeline.findTransform("first");
    TransformMeta second = pipeline.findTransform("second");

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

  @Test
  void undoesAndRedoesTransformDeleteWithAttachedHop() {
    PipelineMeta pipeline = pipelineWithTwoTransforms();
    PipelineEditor editor = new PipelineEditor(pipeline);
    assertNotNull(editor.addHop("first", "second"));
    pipeline.clearUndo();

    assertEquals(true, editor.deleteTransform("first"));
    assertEquals(true, editor.undo());
    assertNotNull(pipeline.findTransform("first"));
    assertEquals(1, pipeline.nrPipelineHops());
    assertEquals(true, editor.redo());
    assertEquals(null, pipeline.findTransform("first"));
    assertEquals(0, pipeline.nrPipelineHops());
  }

  @Test
  void undoesAndRedoesMoveAndHopMutation() {
    PipelineMeta pipeline = pipelineWithTwoTransforms();
    PipelineEditor editor = new PipelineEditor(pipeline);

    assertEquals(1, editor.moveTransforms(List.of("first"), 5, 6));
    assertEquals(true, editor.undo());
    assertEquals(new Point(10, 20), pipeline.findTransform("first").getLocation());
    assertEquals(true, editor.redo());
    assertEquals(new Point(15, 26), pipeline.findTransform("first").getLocation());

    assertNotNull(editor.addHop("first", "second"));
    pipeline.clearUndo();
    assertEquals(true, editor.setHopEnabled("first", "second", false));
    assertEquals(true, editor.undo());
    assertEquals(
        true,
        pipeline
            .findPipelineHop(
                pipeline.findTransform("first"), pipeline.findTransform("second"), true)
            .isEnabled());
    assertEquals(true, editor.redo());
    assertEquals(
        false,
        pipeline
            .findPipelineHop(
                pipeline.findTransform("first"), pipeline.findTransform("second"), true)
            .isEnabled());
  }

  @Test
  void undoRedoReturnFalseWhenHistoryIsEmpty() {
    PipelineEditor editor = new PipelineEditor(pipelineWithTwoTransforms());
    assertEquals(false, editor.undo());
    assertEquals(false, editor.redo());
  }

  private static PipelineMeta pipelineWithTwoTransforms() {
    PipelineMeta pipeline = new PipelineMeta();
    pipeline.addTransform(transform("first", 10, 20));
    pipeline.addTransform(transform("second", 30, 40));
    pipeline.clearUndo();
    pipeline.clearChanged();
    return pipeline;
  }

  private static TransformMeta transform(String name, int x, int y) {
    TransformMeta transform = new TransformMeta();
    transform.setName(name);
    transform.setLocation(x, y);
    return transform;
  }
}
