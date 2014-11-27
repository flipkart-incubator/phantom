/*
 * Copyright 2012-2015, the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package com.flipkart.phantom.task.spi.tracing;

/**
 * <code>TraceData</code> holds information for a trace
 * 
 * @author Regunath B
 * @version 1.0, 25th Nov, 2014
 */
public class TraceData {

    private Long traceId;
    private Long spanId;
    private Long parentSpanId;
    private Boolean shouldBeSampled;
    private String spanName;

    public void setTraceId(final Long traceId) {
        if (traceId != null) {
            this.traceId = traceId;
        }
    }

    public void setSpanId(final Long spanId) {
        if (spanId != null) {
            this.spanId = spanId;
        }
    }

    public void setParentSpanId(final Long parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    public void setShouldBeSampled(final Boolean shouldBeSampled) {
        this.shouldBeSampled = shouldBeSampled;
    }

    public void setSpanName(final String spanName) {
        this.spanName = spanName;
    }

    public Long getTraceId() {
        return traceId;
    }

    public Long getSpanId() {
        return spanId;
    }

    public Long getParentSpanId() {
        return parentSpanId;
    }

    public Boolean shouldBeTraced() {
        return shouldBeSampled;
    }

    public String getSpanName() {
        return spanName;
    }

}
