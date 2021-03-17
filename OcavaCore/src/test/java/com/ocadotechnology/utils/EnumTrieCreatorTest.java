/*
 * Copyright © 2017-2021 Ocado (Ocava)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ocadotechnology.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.utils.TrieNode.Branch;
import com.ocadotechnology.utils.TrieNode.Leaf;
import com.ocadotechnology.utils.TrieNodeSearchTest.SimpleEnum;

class EnumTrieCreatorTest {

    @Test
    void onlyLeafNodesCreatedForSimpleEnum() {
        TrieNode<Enum<?>> simpleTrie = EnumTrieCreator.create(SimpleEnum.class);
        assertThat(simpleTrie).isInstanceOf(Branch.class);

        Map<String, ? extends TrieNode<?>> branches = ((Branch<?>) simpleTrie).branches;
        assertThat(branches).hasSize(2);
        assertThat(branches.values()).allMatch(n -> n instanceof TrieNode.Leaf);
        assertThat(branches.get("YES")).isEqualTo(new Leaf<>(SimpleEnum.YES));
        assertThat(branches.get("NO")).isEqualTo(new Leaf<>(SimpleEnum.NO));
    }

    @Test
    void noEntriesForEmptyEnum() {
        TrieNode<Enum<?>> enumNode = EnumTrieCreator.create(EmptyEnum.class);
        assertThat(enumNode).isInstanceOf(Branch.class);

        Branch<Enum<?>> node = (Branch<Enum<?>>) enumNode;
        assertThat(node.name).isEqualTo(EmptyEnum.class.getSimpleName());
        assertThat(node.branches).isEmpty();
    }

    private enum EmptyEnum {

    }
}
