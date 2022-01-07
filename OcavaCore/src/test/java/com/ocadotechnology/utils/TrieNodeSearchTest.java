/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import org.junit.jupiter.api.Test;

class TrieNodeSearchTest {

    private static final TrieNode<Enum<?>> simpleTrie = EnumTrieCreator.create(SimpleEnum.class);
    private static final TrieNode<Enum<?>> complexTrie = EnumTrieCreator.create(ComplexEnum.class);

    @Test
    void testSearchForEmptyStringIsEmpty() {
        assertThat(simpleTrie.search("")).isEmpty();
    }

    @Test
    void testSearchForOnlyBranchOrLeafRatherThanFullPrefixIsEmpty() {
        assertThat(simpleTrie.search("SimpleEnum")).isEmpty();
        assertThat(simpleTrie.search("NO")).isEmpty();
    }

    @Test
    void testSearchForSimpleTrieReturnsValue() {
        assertThat(simpleTrie.search("SimpleEnum.NO")).hasValue(SimpleEnum.NO);
    }

    @Test
    void testSearchForNestedTrieReturnsValue() {
        assertThat(complexTrie.search("ComplexEnum.NestedEnum.MAYBE")).hasValue(ComplexEnum.NestedEnum.MAYBE);
    }

    @Test
    void testSearchForNonExistentValueIsEmpty() {
        // Check no leaf
        assertThat(simpleTrie.search("SimpleEnum.MAYBE")).isEmpty();
        assertThat(complexTrie.search("ComplexEnum.NestedEnum.NO")).isEmpty();

        // Check wrong prefix for trie
        assertThat(simpleTrie.search("ComplexEnum.YES")).isEmpty();

        // Check no branch
        assertThat(simpleTrie.search("SimpleEnum.NestedEnum.MAYBE")).isEmpty();
    }

    enum SimpleEnum {
        YES,
        NO
    }

    private enum ComplexEnum {
        YES,
        NO;

        enum NestedEnum {
            MAYBE
        }
    }
}
