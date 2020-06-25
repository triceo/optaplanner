/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.config.util;

import org.optaplanner.core.config.heuristic.selector.move.MoveSelectorConfig;

/**
 * Constants used for centralizing {@link MoveSelectorConfig} XML element names.
 */
public interface MoveSelectorElementNames {
    String CARTESIAN_PRODUCT_MOVE_SELECTOR = "cartesianProductMoveSelector";
    String CHANGE_MOVE_SELECTOR = "changeMoveSelector";
    String K_OPT_MOVE_SELECTOR = "kOptMoveSelector";
    String MOVE_ITERATOR_FACTORY = "moveIteratorFactory";
    String MOVE_LIST_FACTORY = "moveListFactory";
    String PILLAR_CHANGE_MOVE_SELECTOR = "pillarChangeMoveSelector";
    String PILLAR_SWAP_MOVE_SELECTOR = "pillarSwapMoveSelector";
    String SUB_CHAIN_CHANGE_MOVE_SELECTOR = "subChainChangeMoveSelector";
    String SUB_CHAIN_SWAP_MOVE_SELECTOR = "subChainSwapMoveSelector";
    String SWAP_MOVE_SELECTOR = "swapMoveSelector";
    String TAIL_CHAIN_SWAP_MOVE_SELECTOR = "tailChainSwapMoveSelector";
    String UNION_MOVE_SELECTOR = "unionMoveSelector";
}
