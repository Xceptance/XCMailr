/**  
 *  Copyright 2013 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package controllers.restapi.util;

import com.google.inject.Singleton;

import filters.AcceptJsonFilter;
import filters.ApiTokenFilter;
import ninja.FilterWith;

/**
 * Base class of all REST API controllers.
 */
@Singleton
@FilterWith(
    {
      ApiTokenFilter.class, // checks the API token 
      AcceptJsonFilter.class // checks that JSON is sent and accepted by the client
    })
public class AbstractApiController
{
}
