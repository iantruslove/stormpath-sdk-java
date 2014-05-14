/*
 * Copyright 2014 Stormpath, Inc.
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
package com.stormpath.sdk.impl.authc;

import com.stormpath.sdk.authc.AuthenticationResult;
import com.stormpath.sdk.resource.Resource;

/**
 * Since {@link AuthenticationResult} is not a resource per-se we use this helper class which is in charge of
 * instantiating the result of an {@link com.stormpath.sdk.application.Application#authenticateAccount(com.stormpath.sdk.authc.AuthenticationRequest)
 * authentication request} obtained from Stormpath.
 *
 * @since 1.0.RC
 */
public interface AuthenticationResultHelper extends Resource {

    AuthenticationResult getAuthenticationResult();

}
