/*
 * Copyright 2021 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.minima.system.network.base.ssz;

import java.util.function.Supplier;
// import tech.pegasys.teku.ssz.SszCollection;
// import tech.pegasys.teku.ssz.SszData;
// import tech.pegasys.teku.ssz.cache.IntCache;
// import tech.pegasys.teku.ssz.schema.SszCollectionSchema;
// import tech.pegasys.teku.ssz.schema.SszCompositeSchema;
// import tech.pegasys.teku.ssz.schema.SszSchema;
// import tech.pegasys.teku.ssz.tree.TreeNode;

public abstract class AbstractSszCollection<SszElementT extends SszData>
    extends AbstractSszComposite<SszElementT> implements SszCollection<SszElementT> {

  protected AbstractSszCollection(
      SszCompositeSchema<?> schema, Supplier<TreeNode> lazyBackingNode) {
    super(schema, lazyBackingNode);
  }

  protected AbstractSszCollection(SszCompositeSchema<?> schema, TreeNode backingNode) {
    super(schema, backingNode);
  }

  protected AbstractSszCollection(
      SszCompositeSchema<?> schema, TreeNode backingNode, IntCache<SszElementT> cache) {
    super(schema, backingNode, cache);
  }

  @SuppressWarnings("unchecked")
  @Override
  public SszCollectionSchema<SszElementT, ?> getSchema() {
    return (SszCollectionSchema<SszElementT, ?>) super.getSchema();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected SszElementT getImpl(int index) {
    SszCollectionSchema<SszElementT, ?> type =
        (SszCollectionSchema<SszElementT, ?>) this.getSchema();
    SszSchema<?> elementType = type.getElementSchema();
    TreeNode node =
        getBackingNode().get(type.getChildGeneralizedIndex(index / type.getElementsPerChunk()));
    return (SszElementT)
        elementType.createFromBackingNode(node, index % type.getElementsPerChunk());
  }
}
