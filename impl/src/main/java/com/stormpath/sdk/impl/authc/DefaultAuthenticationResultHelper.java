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
import com.stormpath.sdk.impl.ds.InternalDataStore;
import com.stormpath.sdk.impl.resource.AbstractResource;
import com.stormpath.sdk.impl.resource.Property;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @since 1.0.RC
 */
public class DefaultAuthenticationResultHelper extends AbstractResource implements AuthenticationResultHelper {

    private static final String AUTHENTICATION_RESULT = "authenticationResult";

    private static final Map<String,Property> PROPERTY_DESCRIPTORS = createPropertyDescriptorMap();

    public DefaultAuthenticationResultHelper(InternalDataStore dataStore) {
        super(dataStore);
    }

    public DefaultAuthenticationResultHelper(InternalDataStore dataStore, Map<String, Object> properties) {
        super(dataStore);
        if (properties != null && properties.size() > 0) {
            Map<String, Object> authenticationResultProperties = new LinkedHashMap<String, Object>();
            authenticationResultProperties.put(DefaultAuthenticationResult.ACCOUNT.getName(), properties.get(DefaultAuthenticationResult.ACCOUNT.getName()));
            DefaultAuthenticationResult authenticationResult = new DefaultAuthenticationResult(dataStore, authenticationResultProperties);
            setProperty(AUTHENTICATION_RESULT, authenticationResult);
        }
    }

    @Override
    public Map<String, Property> getPropertyDescriptors() {
        return PROPERTY_DESCRIPTORS;
    }

    @Override
    public AuthenticationResult getAuthenticationResult() {
        return (AuthenticationResult) getProperty(AUTHENTICATION_RESULT);
    }

}
