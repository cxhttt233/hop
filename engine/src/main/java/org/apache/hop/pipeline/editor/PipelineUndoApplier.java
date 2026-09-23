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

import org.apache.hop.core.gui.Point;
import org.apache.hop.core.undo.ChangeAction;
import org.apache.hop.pipeline.PipelineHopMeta;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

/** Applies pipeline {@link ChangeAction} instances without any SWT dependency. */
final class PipelineUndoApplier {
  private final PipelineMeta pipeline;

  PipelineUndoApplier(PipelineMeta pipeline) {
    this.pipeline = pipeline;
  }

  void apply(ChangeAction action, boolean undo) {
    switch (action.getType()) {
      case NewTransform -> applyTransformCreateDelete(action, undo);
      case DeleteTransform -> applyTransformCreateDelete(action, !undo);
      case NewPipelineHop -> applyHopCreateDelete(action, undo);
      case DeletePipelineHop -> applyHopCreateDelete(action, !undo);
      case ChangePipelineHop -> applyHopChange(action, undo);
      case PositionTransform -> applyTransformPosition(action, undo);
      default ->
          throw new IllegalArgumentException(
              "Unsupported pipeline undo action: " + action.getType());
    }
    pipeline.setChanged();
  }

  private void applyTransformCreateDelete(ChangeAction action, boolean remove) {
    Object[] values = action.getCurrent();
    int[] indexes = action.getCurrentIndex();
    for (int i = remove ? values.length - 1 : 0;
        remove ? i >= 0 : i < values.length;
        i += remove ? -1 : 1) {
      TransformMeta transform = (TransformMeta) values[i];
      if (remove) {
        int position = pipeline.indexOfTransform(transform);
        if (position >= 0) pipeline.removeTransform(position);
      } else pipeline.addTransform(indexes[i], transform);
    }
  }

  private void applyHopCreateDelete(ChangeAction action, boolean remove) {
    Object[] values = action.getCurrent();
    int[] indexes = action.getCurrentIndex();
    for (int i = remove ? values.length - 1 : 0;
        remove ? i >= 0 : i < values.length;
        i += remove ? -1 : 1) {
      PipelineHopMeta hop = (PipelineHopMeta) values[i];
      if (remove) {
        PipelineHopMeta existing =
            pipeline.findPipelineHop(hop.getFromTransform(), hop.getToTransform());
        if (existing != null) pipeline.removePipelineHop(existing);
      } else {
        pipeline.addPipelineHop(indexes[i], hop);
      }
    }
  }

  private void applyHopChange(ChangeAction action, boolean undo) {
    Object[] values = undo ? action.getPrevious() : action.getCurrent();
    int[] indexes = undo ? action.getPreviousIndex() : action.getCurrentIndex();
    for (int i = 0; i < values.length; i++) {
      PipelineHopMeta snapshot = (PipelineHopMeta) values[i];
      pipeline.getPipelineHops().set(indexes[i], snapshot.clone());
    }
  }

  private void applyTransformPosition(ChangeAction action, boolean undo) {
    Object[] values = action.getCurrent();
    Point[] locations = undo ? action.getPreviousLocation() : action.getCurrentLocation();
    for (int i = 0; i < values.length; i++) {
      ((TransformMeta) values[i]).setLocation(new Point(locations[i].x, locations[i].y));
    }
  }
}
