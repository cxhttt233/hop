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
import org.apache.hop.core.NotePadMeta;
import org.apache.hop.pipeline.PipelineHopMeta;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

/** Stable, UI-independent graph projection of a pipeline document. */
public final class PipelineGraphProjection {
  private static final String TRANSFORM_KIND = "transform";
  private static final String NOTE_KIND = "note";

  private PipelineGraphProjection() {}

  public static Graph project(PipelineMeta pipelineMeta) {
    Objects.requireNonNull(pipelineMeta, "pipelineMeta");
    List<Node> nodes = new ArrayList<>();
    pipelineMeta.getTransforms().stream().map(PipelineGraphProjection::node).forEach(nodes::add);
    for (int i = 0; i < pipelineMeta.getNotes().size(); i++) {
      nodes.add(note(i, pipelineMeta.getNotes().get(i)));
    }
    List<Edge> edges =
        pipelineMeta.getPipelineHops().stream().map(PipelineGraphProjection::edge).toList();
    return new Graph(List.copyOf(nodes), edges);
  }

  private static Node node(TransformMeta transform) {
    return new Node(
        transform.getName(),
        transform.getName(),
        TRANSFORM_KIND,
        transform.getTransformPluginId(),
        TRANSFORM_KIND,
        transform.getLocation().x,
        transform.getLocation().y);
  }

  private static Node note(int index, NotePadMeta note) {
    return new Node(
        "note:" + index,
        note.getNote(),
        NOTE_KIND,
        null,
        null,
        note.getLocation().x,
        note.getLocation().y);
  }

  private static Edge edge(PipelineHopMeta hop) {
    String source = hop.getFromTransform().getName();
    String target = hop.getToTransform().getName();
    return new Edge(source + "->" + target, source, target, hop.isEnabled());
  }

  public record Graph(List<Node> nodes, List<Edge> edges) {}

  public record Node(
      String id, String name, String kind, String pluginId, String pluginType, int x, int y) {}

  public record Edge(String id, String source, String target, boolean enabled) {}
}
