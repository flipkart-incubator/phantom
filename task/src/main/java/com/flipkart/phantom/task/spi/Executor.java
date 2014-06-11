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

package com.flipkart.phantom.task.spi;

import com.flipkart.phantom.event.ServiceProxyEvent;

/**
 * <code>Executor</code> is an interface which executes any task submitted of Type T.
 * This interface provides a way of decoupling task submission from the mechanics
 * of how each task will be run
 *
 * @author : arya.ketan
 * @version : 1.0
 * @date : 28/10/13
 */
public interface Executor<T>{
    public T execute() ;

    public ServiceProxyEvent.Builder getEventBuilder();
}
