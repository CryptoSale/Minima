/*
 * Copyright 2020 ConsenSys AG.
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

// import tech.pegasys.teku.ssz.SszData;
// import tech.pegasys.teku.ssz.SszMutableRefVector;
// import tech.pegasys.teku.ssz.SszMutableVector;
// import tech.pegasys.teku.ssz.SszVector;
// import tech.pegasys.teku.ssz.cache.IntCache;
// import tech.pegasys.teku.ssz.schema.SszVectorSchema;
// import tech.pegasys.teku.ssz.tree.TreeNode;

public class SszMutableVectorImpl<
        SszElementT extends SszData, SszMutableElementT extends SszElementT>
    extends AbstractSszMutableCollection<SszElementT, SszMutableElementT>
    implements SszMutableRefVector<SszElementT, SszMutableElementT> {

  public SszMutableVectorImpl(AbstractSszComposite<SszElementT> backingImmutableData) {
    super(backingImmutableData);
  }

  @Override
  protected AbstractSszComposite<SszElementT> createImmutableSszComposite(
      TreeNode backingNode, IntCache<SszElementT> childrenCache) {
    return new SszVectorImpl<>(getSchema(), backingNode, childrenCache);
  }

  @Override
  @SuppressWarnings("unchecked")
  public SszVectorSchema<SszElementT, ?> getSchema() {
    return (SszVectorSchema<SszElementT, ?>) super.getSchema();
  }

  @Override
  @SuppressWarnings("unchecked")
  public SszVector<SszElementT> commitChanges() {
    return (SszVector<SszElementT>) super.commitChanges();
  }

  @Override
  protected void checkIndex(int index, boolean set) {
    if (index < 0 || index >= size()) {
      throw new IndexOutOfBoundsException(
          "Invalid index " + index + " for vector with size " + size());
    }
  }

  @Override
  public SszMutableVector<SszElementT> createWritableCopy() {
    throw new UnsupportedOperationException();
  }
}
