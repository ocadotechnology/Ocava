/*
 * Copyright © 2017-2024 Ocado (Ocava)
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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Relational hierarchy structure that allows for the storage and quick lookup of values via prefixes.
 * Form of values is ParentA.ParentB.Child1 for example, where search("Child1") will return Child1.
 * @see <a href="https://en.wikipedia.org/wiki/Trie">Trie structure</a> for more information
 */
@ParametersAreNonnullByDefault
public interface TrieNode<E> {
    /**
     * Search the current trie node for the string value. Case sensitive.
     * Will return {@link Optional#empty()} if value not found.
     */
    Optional<E> search(String path);

    /**
     * Single value node.
     */
    class Leaf<E> implements TrieNode<E> {
        E value;

        Leaf(E value) {
            this.value = value;
        }

        @Override
        public Optional<E> search(String name) {
            return Optional.ofNullable(value.toString().equals(name) ? value : null);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Leaf<?> leaf = (Leaf<?>) o;
            return Objects.equals(value, leaf.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /**
     * Nested node.
     * Contains a map of values that allows for recursive structure, representing the list of branches.
     * The name is the current parent label.
     */
    class Branch<E> implements TrieNode<E> {
        String name;
        Map<String, TrieNode<E>> branches;

        Branch(String name, Map<String, TrieNode<E>> branches) {
            this.name = name;
            this.branches = branches;
        }

        /**
         * Recursively searches through current node down the branches matching the full provided path until it finds
         * a matching leaf node. It will not return a partial match against a branch. If at any point the path is
         * invalid (i.e. referencing a nested class that does not exist) then it will terminate and return
         * {@link Optional#empty()}.
         *
         * Returns {@link Optional#empty()} if no value found.
         */
        @Override
        public Optional<E> search(String pathToValue) {
            String[] parts = pathToValue.split("\\.", 3);
            if (parts.length <= 1) {
                return Optional.empty();
            }
            if (!name.equals(parts[0])) {
                return Optional.empty();
            }
            TrieNode<E> trieNode = branches.get(parts[1]);
            String nextPathSection = parts.length > 2 ? parts[1] + "." + parts[2] : parts[1];
            return Optional.ofNullable(trieNode)
                    .flatMap(n -> n.search(nextPathSection));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Branch<?> node = (Branch<?>) o;
            return Objects.equals(branches, node.branches);
        }

        @Override
        public int hashCode() {
            return Objects.hash(branches);
        }
    }
}