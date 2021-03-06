/*
 * Copyright (C) 2014 Indeed Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package com.indeed.imhotep.local;

import java.io.Serializable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import com.indeed.flamdex.api.IntValueLookup;

@VisibleForTesting
public class DynamicMetric implements IntValueLookup, Serializable {
    private static final long serialVersionUID = 1L;
    private final int[] values;

    public DynamicMetric(int size) {
        this.values = new int[size];
    }

    @Override
    public long getMin() {
        return Ints.min(values);
    }

    @Override
    public long getMax() {
        return Ints.max(values);
    }

    @Override
    public void lookup(int[] docIds, long[] values, int n) {
        for (int i = 0; i < n; i++) {
            values[i] = this.values[docIds[i]];
        }
    }

    @Override
    public long memoryUsed() {
        return 4L * values.length;
    }

    @Override
    public void close() {
        // simply popping this from the metric stack doesn't have any effect
    }

    public void add(int doc, int delta) {
        // TODO optimize this to remove branches
        final long newValue = (long) values[doc] + (long) delta;
        if (newValue < Integer.MIN_VALUE) {
            values[doc] = Integer.MIN_VALUE;
        } else if (newValue > Integer.MAX_VALUE) {
            values[doc] = Integer.MAX_VALUE;
        } else {
            values[doc] = (int) newValue;
        }
    }
    
    public void set(int doc, int value) {
        values[doc] = value;
    }

    public int lookupSingleVal(int docId) {
        return this.values[docId];
    }

}