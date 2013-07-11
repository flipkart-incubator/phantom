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

package com.flipkart.sp.task.spi.task;

import java.util.Arrays;
import java.util.List;

/**
 * Result of a task handler invocation.
 *
 * @author devashishshankar
 * @version 1.0, 19 March, 2013
 */
public class TaskResult {
	
	/** The default length in case the Object isn't a data array */
	private static final int DEFAULT_LENGTH = 0;
	
	private final boolean success;
	private final String message;
	private final Object data;
    private final List<Object> dataList;
    private final int length;
    private boolean profilingDone = false;
    
    /** Various constructors for this class*/    
	public TaskResult(boolean success, String message) {
		this(success, message, (byte[])null);
	}

	public TaskResult(boolean success, String message, Object data) {
		this.success = success;
		this.message = message;
		this.data = data;
        this.dataList = null;
        if(data instanceof byte[]) {
        	byte[] dataBytes = (byte[]) data;
        	this.length= (dataBytes == null ? 0 : dataBytes.length);
        } else {
        	this.length = TaskResult.DEFAULT_LENGTH;
        }
	}

    public TaskResult(boolean success, String message, List<Object> dataArray,int length) {
        this.success = success;
        this.message = message;
        this.data = null;
        this.dataList = Arrays.asList(dataArray.toArray());
        this.length=length;
    }

    public TaskResult(boolean success, String message, byte[] data, boolean profilingDone) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.dataList = null;
        this.length= (data == null ? 0 : data.length);
        this.profilingDone = profilingDone;
    }

    /**
     * Overriden super class method. Returns a string representation of this TaskResult
     * @see java.lang.Object#toString()
     */
    public String toString() {
    	return String.format("TaskResult[success:%s, message:%s,datalength:%d]", this.success, this.message, this.length);
    }

    /** Getter/Setter methods*/
    public int getLength() {
        return length;
    }
    public boolean isDataArray() {
        return dataList != null;
    }
	public boolean isSuccess() {
		return success;
	}
	public String getMessage() {
		return message;
	}
	public Object getData() {
		return data;
	}
    public List<Object> getDataArray() {
        return dataList;
    }
    public boolean isProfilingDone() {
        return profilingDone;
    }
    /** End Getter/Setter methods*/
}