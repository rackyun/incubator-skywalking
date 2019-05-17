/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package org.apache.skywalking.apm.util;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author yunhai.hu
 * at 2019/5/10
 */
public class ArrayUtilsTest {

    @Test
    public void merge() {

        String[] arr1 = new String[]{"1", "2", "3"};
        String[] arr2 = new String[]{"a", "b"};
        String[] merged = ArrayUtils.merge(arr1, arr2);
        assertEquals(5, merged.length);
        assertArrayEquals(new String[]{"1", "2", "3", "a", "b"}, merged);
    }
}