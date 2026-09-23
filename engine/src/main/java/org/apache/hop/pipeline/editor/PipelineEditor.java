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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.hop.base.AbstractMeta;
import org.apache.hop.core.gui.Point;
import org.apache.hop.pipeline.PipelineHopMeta;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

/** SWT-free editing operations shared by web and desktop pipeline editors. */
public class PipelineEditor {
  private final PipelineMeta pipelineMeta;

  public PipelineEditor(PipelineMeta pipelineMeta) {
    this.pipelineMeta = Objects.requireNonNull(pipelineMeta, "pipelineMeta");
  }

  /** Adds a hop between two existing transforms as one undoable operation. */
  public PipelineHopMeta addHop(String fromName, String toName) {
    TransformMeta from = pipelineMeta.findTransform(fromName);
    TransformMeta to = pipelineMeta.findTransform(toName);
    if (from == null || to == null || from == to || pipelineMeta.findPipelineHop(from, to) != null) {
      return null;
    }

    PipelineHopMeta hop = new PipelineHopMeta(from, to);
    pipelineMeta.addPipelineHop(hop);
    int position = pipelineMeta.getPipelineHops().indexOf(hop);
    pipelineMeta.addUndo(
        new Object[] {hop}, null, new int[] {position}, null, null, AbstractMeta.TYPE_UNDO_NEW, false);
    pipelineMeta.setChanged();
    return hop;
  }

  /** Removes an existing hop as one undoable operation. */
  public boolean deleteHop(String fromName, String toName) {
    TransformMeta from = pipelineMeta.findTransform(fromName);
    TransformMeta to = pipelineMeta.findTransform(toName);
    if (from == null || to == null) {
      return false;
    }
    PipelineHopMeta hop = pipelineMeta.findPipelineHop(from, to);
    if (hop == null) {
      return false;
    }

    int position = pipelineMeta.getPipelineHops().indexOf(hop);
    pipelineMeta.removePipelineHop(hop);
    pipelineMeta.addUndo(
        new Object[] {hop}, null, new int[] {position}, null, null, AbstractMeta.TYPE_UNDO_DELETE, false);
    pipelineMeta.setChanged();
    return true;
  }

  /** Moves the named transforms as one undoable operation. */
  public int moveTransforms(List<String> names, int dx, int dy) {
    if (names == null || names.isEmpty() || (dx == 0 && dy == 0)) {
      return 0;
    }

    List<TransformMeta> transforms = new ArrayList<>();
    List<Point> previous = new ArrayList<>();
    List<Point> current = new ArrayList<>();
    List<Integer> positions = new ArrayList<>();

    for (String name : names) {
      TransformMeta transform = pipelineMeta.findTransform(name);
      if (transform == null || transforms.contains(transform)) {
        continue;
      }
      Point before = transform.getLocation();
      Point after = new Point(Math.max(0, before.x + dx), Math.max(0, before.y + dy));
      if (before.equals(after)) {
        continue;
      }
      transforms.add(transform);
      previous.add(new Point(before.x, before.y));
      current.add(after);
      positions.add(pipelineMeta.indexOfTransform(transform));
    }

    if (transforms.isEmpty()) {
      return 0;
    }
    for (int i = 0; i < transforms.size(); i++) {
      transforms.get(i).setLocation(current.get(i));
    }
    pipelineMeta.addUndo(
        transforms.toArray(),
        null,
        positions.stream().mapToInt(Integer::intValue).toArray(),
        previous.toArray(Point[]::new),
        current.toArray(Point[]::new),
        AbstractMeta.TYPE_UNDO_POSITION,
        false);
    pipelineMeta.setChanged();
    return transforms.size();
  }
}
