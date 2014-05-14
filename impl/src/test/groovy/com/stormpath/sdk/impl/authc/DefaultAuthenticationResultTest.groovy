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
package com.stormpath.sdk.impl.authc

import com.stormpath.sdk.account.Account
import com.stormpath.sdk.impl.ds.InternalDataStore
import com.stormpath.sdk.impl.resource.ResourceReference
import org.testng.annotations.Test

import static org.easymock.EasyMock.*
import static org.testng.Assert.assertEquals
import static org.testng.Assert.*

/**
 * @since 1.0.RC
 */
class DefaultAuthenticationResultTest {

    @Test
    void testGetPropertyDescriptors() {

        def authenticationResult = new DefaultAuthenticationResult(createStrictMock(InternalDataStore))

        def propertyDescriptors = authenticationResult.getPropertyDescriptors()

        assertEquals(propertyDescriptors.size(), 1)

        assertTrue(propertyDescriptors.get("account") instanceof ResourceReference && propertyDescriptors.get("account").getType().equals(Account))
    }

    @Test
    void testInstantiation() {

        def properties = [account: [href: "https://api.stormpath.com/v1/accounts/iouertnw48ufsjnsDFSf"]]

        def internalDataStore = createStrictMock(InternalDataStore)
        def account = createStrictMock(Account)

        expect(internalDataStore.instantiate(Account, properties.account)).andReturn(account)

        replay internalDataStore

        def authenticationResult = new DefaultAuthenticationResult(internalDataStore, properties)

        assertEquals(authenticationResult.getAccount(), account)

        verify internalDataStore
    }

    @Test
    void testInstantiationEmptyProperties() {

        def internalDataStore = createStrictMock(InternalDataStore)

        def properties = new HashMap<String, Object>()

        def authenticationResult = new DefaultAuthenticationResult(internalDataStore, properties)
        assertNull(authenticationResult.getAccount())
    }

}
