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
package com.ocadotechnology.scenario;

import java.lang.reflect.Field;

public class LoggerUtil {

    private LoggerUtil() {
    }

    public static String getLocalFields(Object object) {
        Field[] fields = object.getClass().getDeclaredFields();
        StringBuilder buffer = new StringBuilder();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                String fieldName = field.getName();
                if (fieldName.contains("val")) {
                    buffer.append(fieldName.replace("val$", ""));
                    buffer.append(" = ");
                    //buffer.append(field.get(object).getClass().getSimpleName());
                    Object val = field.get(object);
                    buffer.append(val == null ? null : val.toString());
                    buffer.append(", ");
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                buffer.append("Can not retrieve fields ");
                buffer.append(e.getMessage());
            }
        }
        return buffer.toString();
    }

    /**
     * Creates link to test method. Eclipse console link format is used (fileName.java:lineNumber).
     */
    public static String getStepName() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String methodName = "";
        String fileName = "";
        int lineNumber = 0;
        int storyLevel = 0;
        boolean hasStoryContentRootBeenObserved = false;

        /**
         *  After the first `PreviousClassStoryContent` continue iterating until either one of the bellow:
         *      - StoryContent / Story / DefaultStory is encountered
         *      - a class without PreviousClassStoryContent is encountered
         *      - end of stack trace
         */
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            try {
                Class<?> act = Class.forName(stackTraceElement.getClassName());
                if (act.isAnnotationPresent(StoryContent.class)  || act.isAnnotationPresent(Story.class) || act.isAnnotationPresent(DefaultStory.class)) {
                    break;
                } else if (act.isAnnotationPresent(PreviousClassStoryContent.class)) {
                    hasStoryContentRootBeenObserved = true;
                } else if (hasStoryContentRootBeenObserved) {
                    break;
                }
            } catch (ClassNotFoundException e) {
                // Ignore and try the next class
            }
            storyLevel++;
        }

        if (stackTraceElements.length > storyLevel) {
            StackTraceElement stackTraceElement = stackTraceElements[storyLevel];

            methodName = stackTraceElements[storyLevel - 1].getMethodName();
            fileName = stackTraceElement.getFileName();
            lineNumber = stackTraceElement.getLineNumber();
        }

        return String.format("(%s:%d).%s", fileName, lineNumber, methodName);
    }

    public static Class<?> getStepClass() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        for (StackTraceElement stackTraceElement : stackTraceElements) {
            try {
                Class<?> act = Class.forName(stackTraceElement.getClassName());
                if (act.isAnnotationPresent(Story.class)) {
                    return act;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
        return null;
    }
}
