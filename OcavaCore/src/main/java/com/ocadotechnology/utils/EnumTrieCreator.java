/*
 * Copyright © 2017-2019 Ocado (Ocava)
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

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.ocadotechnology.utils.TrieNode.Branch;
import com.ocadotechnology.utils.TrieNode.Leaf;

/**
 * Creator for hierarchical structure storing nested enum classes.
 * Can be searched for a string, using the fully qualified class name, to find the matching Enum.
 * Structure will recursively search as needed.
 */
public class EnumTrieCreator {

    private EnumTrieCreator() {}

    /**
     * Returns initial parent node of hierarchy which contains the name of the provided class and any branches
     * built from enum values and nested classes.
     */
    public static TrieNode<Enum<?>> create(Class<? extends Enum<?>> firstClass) {
        return new Branch<>(firstClass.getSimpleName(), generateNodes(firstClass));
    }

    @SuppressWarnings("unchecked") // We do check that the class is an enum class before casting
    private static Map<String, TrieNode<Enum<?>>> generateNodes(Class<? extends Enum<?>> cls) {
        Map<String, TrieNode<Enum<?>>> children = new HashMap<>();
        Enum<?>[] enums = cls.getEnumConstants();
        for (Enum<?> anEnum : enums) {
            children.put(anEnum.name(), new Leaf<>(anEnum));
        }

        Class[] nestedClasses = cls.getDeclaredClasses();
        for (Class<?> subEnum : nestedClasses) {
            Preconditions.checkState(subEnum.isEnum());
            children.put(subEnum.getSimpleName(), new Branch<>(subEnum.getSimpleName(), generateNodes((Class<Enum<?>>) subEnum)));
        }

        return children;
    }
}