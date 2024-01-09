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
package com.ocadotechnology.indexedcache;

// Used for testing the indexed cache
class CoordinateLikeTestObject {
    static final CoordinateLikeTestObject ORIGIN = new CoordinateLikeTestObject(0, 0);

    final int x;
    final int y;

    static CoordinateLikeTestObject create(int x, int y) {
        return new CoordinateLikeTestObject(x, y);
    }

    private CoordinateLikeTestObject(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CoordinateLikeTestObject that = (CoordinateLikeTestObject) o;
        return this.x == that.x && this.y == that.y;

    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";  // debugging
    }
}
