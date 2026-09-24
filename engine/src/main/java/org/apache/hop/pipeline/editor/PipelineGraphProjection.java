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

import java.util.List;
import java.util.Objects;
import org.apache.hop.pipeline.PipelineHopMeta;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

/** Stable, UI-independent graph projection of a pipeline document. */
public final class PipelineGraphProjection {
  private PipelineGraphProjection() {}

  public static Graph project(PipelineMeta pipelineMeta) {
    Objects.requireNonNull(pipelineMeta, "pipelineMeta");
    List<Node> nodes = pipelineMeta.getTransforms().stream().map(PipelineGraphProjection::node).toList();
    List<Edge> edges = pipelineMeta.getPipelineHops().stream().map(PipelineGraphProjection::edge).toList();
    return new Graph(nodes, edges);
  }

  private static Node node(TransformMeta transform) {
    return new Node(
        transform.getName(), transform.getLocation().x, transform.getLocation().y, transform.isSelected());
  }

  private static Edge edge(PipelineHopMeta hop) {
    return new Edge(hop.getFromTransform().getName(), hop.getToTransform().getName(), hop.isEnabled());
  }

  public record Graph(List<Node> nodes, List<Edge> edges) {}

  public record Node(String id, int x, int y, boolean selected) {}

  public record Edge(String from, String to, boolean enabled) {}
}
