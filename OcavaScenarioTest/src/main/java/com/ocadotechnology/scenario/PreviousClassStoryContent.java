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
package com.ocadotechnology.scenario;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * Using this annotation will link the output stack trace to the first trace contained in a class above on the stack that does not include this annotation.
 *
 * For example, Class C is annotated with "PreviousClassStoryContent". Class B utilizes a helper function included in Class C to add a check step. If the check step fails,
 * then the output will lead towards the line in Class B that calls the helper function in Class C.
 *<pre>
 *        |---------|
 *        | Class A |    -- @Story -- calls helper functions in class B
 *        |---------|
 *             |
 *             v
 *        |---------|
 *        | Class B |    -- does not require annotation -- calls helper functions in class C
 *        |---------|
 *             |
 *             v
 *        |---------|
 *        | Class C |    -- @PreviousClassStoryContent -- calls addCheckStep
 *        |---------|
 *             |
 *             V
 *    |------------------|
 *    | AbstractThenSteps|
 *    |------------------|
 *</pre>
 */
@Target(value = {ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface PreviousClassStoryContent {
}
