/*
 * Copyright 2012-2015, Flipkart Internet Pvt Ltd. All rights reserved.
 * 
 * This software is the confidential and proprietary information of Flipkart Internet Pvt Ltd. ("Confidential Information").  
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with Flipkart.    
 * 
 */
package com.flipkart.sp.task.impl.task;

import java.util.Arrays;
import java.util.List;

/**
 * Result of a task handler invocation. Based on com.flipkart.w3.agent.TaskResult
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