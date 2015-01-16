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
package com.flipkart.phantom.task.spi.interceptor;

/**
 * <code>SpanNameFilter</code> may be used to filter parts out of span name so zipkin is able to group them more logical.
 * As an example: if you have context/path/id with id as a random UUID path variable, the default span collector will create
 * unique entries in zipkin for each webservice call.
 * 
 * This implementation is based on the Brave SpanNameFilter code.
 * 
 * @author Regunath B
 * @version 1.0, 13th Nov, 2014
 */

public interface SpanNameFilter {
	
    /**
     * Filter the span name.
     * @param unfilteredSpanName the unfiltered span name
     * @return the filtered span name.
     */	
	public String filterSpanName(String unfilteredSpanName);
}
