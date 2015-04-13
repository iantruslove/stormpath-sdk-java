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
package com.stormpath.sdk.impl.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.stormpath.sdk.account.Account
import com.stormpath.sdk.account.Accounts
import com.stormpath.sdk.api.ApiKey
import com.stormpath.sdk.api.ApiKeys
import com.stormpath.sdk.application.AccountStoreMapping
import com.stormpath.sdk.application.AccountStoreMappingList
import com.stormpath.sdk.application.Application
import com.stormpath.sdk.application.Applications
import com.stormpath.sdk.authc.UsernamePasswordRequest
import com.stormpath.sdk.client.AuthenticationScheme
import com.stormpath.sdk.client.ClientIT
import com.stormpath.sdk.directory.Directories
import com.stormpath.sdk.directory.Directory
import com.stormpath.sdk.group.Group
import com.stormpath.sdk.group.Groups
import com.stormpath.sdk.impl.api.ApiKeyParameter
import com.stormpath.sdk.impl.ds.DefaultDataStore
import com.stormpath.sdk.impl.ds.api.ApiKeyCacheParameter
import com.stormpath.sdk.impl.http.authc.SAuthc1RequestAuthenticator
import com.stormpath.sdk.impl.security.ApiKeySecretEncryptionService
import com.stormpath.sdk.provider.GoogleProvider
import com.stormpath.sdk.provider.ProviderAccountRequest
import com.stormpath.sdk.provider.Providers
import com.stormpath.sdk.resource.ResourceException
import com.stormpath.sdk.tenant.Tenant
import org.apache.commons.codec.binary.Base64
import org.testng.annotations.Test

import static org.testng.Assert.*

class ApplicationIT extends ClientIT {

    def encryptionServiceBuilder = new ApiKeySecretEncryptionService.Builder()

    /**
     * Asserts fix for <a href="https://github.com/stormpath/stormpath-sdk-java/issues/17">Issue #17</a>
     */
    @Test
    void testLoginWithCachingEnabled() {

        def username = uniquify('lonestarr')
        def password = 'Changeme1!'

        //we could use the parent class's Client instance, but we re-define it here just in case:
        //if we ever turn off caching in the parent class config, we can't let that affect this test:
        def client = buildClient(true)

        def app = createTempApp()

        def acct = client.instantiate(Account)
        acct.username = username
        acct.password = password
        acct.email = username + '@nowhere.com'
        acct.givenName = 'Joe'
        acct.surname = 'Smith'
        acct = app.createAccount(Accounts.newCreateRequestFor(acct).setRegistrationWorkflowEnabled(false).build())

        def request = new UsernamePasswordRequest(username, password)
        def result = app.authenticateAccount(request)

        def cachedAccount = result.getAccount()

        assertEquals cachedAccount.username, acct.username
    }

    @Test
    void testCreateAppAccount() {

        def app = createTempApp()

        def email = 'deleteme@nowhere.com'

        Account account = client.instantiate(Account)
        account.givenName = 'John'
        account.surname = 'DELETEME'
        account.email =  email
        account.password = 'Changeme1!'

        def created = app.createAccount(account)

        //verify it was created:

        def found = app.getAccounts(Accounts.where(Accounts.email().eqIgnoreCase(email))).iterator().next()
        assertEquals(created.href, found.href)

        //test delete:
        found.delete()

        def list = app.getAccounts(Accounts.where(Accounts.email().eqIgnoreCase(email)))
        assertFalse list.iterator().hasNext() //no results
    }

    @Test
    void testCreateAppGroup() {

        def tenant = client.currentTenant

        def app = client.instantiate(Application)

        def authenticationScheme = client.dataStore.requestExecutor.requestAuthenticator

        //When no authenticationScheme is explicitly defined, SAuthc1RequestAuthenticator is used by default
        assertTrue authenticationScheme instanceof SAuthc1RequestAuthenticator

        app.name = uniquify("DELETEME")

        def dirName = uniquify("DELETEME")

        app = tenant.createApplication(Applications.newCreateRequestFor(app).createDirectoryNamed(dirName).build())
        def dir = tenant.getDirectories(Directories.where(Directories.name().eqIgnoreCase(dirName))).iterator().next()

        deleteOnTeardown(dir)
        deleteOnTeardown(app)

        Group group = client.instantiate(Group)
        group.name = uniquify('DELETEME')

        def created = app.createGroup(group)

        //verify it was created:

        def found = app.getGroups(Groups.where(Groups.name().eqIgnoreCase(group.name))).iterator().next()

        assertEquals(created.href, found.href)

        //test delete:
        found.delete()

        def list = app.getGroups(Groups.where(Groups.name().eqIgnoreCase(group.name)))
        assertFalse list.iterator().hasNext() //no results
    }

    @Test
    void testCreateAppGroupWithSauthc1RequestAuthenticator() {

        //We are creating a new client with Digest Authentication
        def client = buildClient(AuthenticationScheme.SAUTHC1)

        def tenant = client.currentTenant

        def app = client.instantiate(Application)

        def authenticationScheme = client.dataStore.requestExecutor.requestAuthenticator

        assertTrue authenticationScheme instanceof SAuthc1RequestAuthenticator

        app.name = uniquify("DELETEME")

        def dirName = uniquify("DELETEME")

        app = tenant.createApplication(Applications.newCreateRequestFor(app).createDirectoryNamed(dirName).build())
        def dir = tenant.getDirectories(Directories.where(Directories.name().eqIgnoreCase(dirName))).iterator().next()

        deleteOnTeardown(dir)
        deleteOnTeardown(app)

        Group group = client.instantiate(Group)
        group.name = uniquify('DELETEME')

        def created = app.createGroup(group)

        //verify it was created:

        def found = app.getGroups(Groups.where(Groups.name().eqIgnoreCase(group.name))).iterator().next()

        assertEquals(created.href, found.href)

        //test delete:
        found.delete()

        def list = app.getGroups(Groups.where(Groups.name().eqIgnoreCase(group.name)))
        assertFalse list.iterator().hasNext() //no results
    }

    @Test
    void testLoginWithAccountStore() {

        def username = uniquify('lonestarr')
        def password = 'Changeme1!'

        //we could use the parent class's Client instance, but we re-define it here just in case:
        //if we ever turn off caching in the parent class config, we can't let that affect this test:
        def client = buildClient(true)

        def app = createTempApp()

        def acct = client.instantiate(Account)
        acct.username = username
        acct.password = password
        acct.email = username + '@nowhere.com'
        acct.givenName = 'Joe'
        acct.surname = 'Smith'

        Directory dir1 = client.instantiate(Directory)
        dir1.name = uniquify("Java SDK: ApplicationIT.testLoginWithAccountStore")
        dir1 = client.currentTenant.createDirectory(dir1);
        deleteOnTeardown(dir1)

        Directory dir2 = client.instantiate(Directory)
        dir2.name = uniquify("Java SDK: ApplicationIT.testLoginWithAccountStore")
        dir2 = client.currentTenant.createDirectory(dir2);
        deleteOnTeardown(dir2)

        AccountStoreMapping accountStoreMapping1 = client.instantiate(AccountStoreMapping)
        accountStoreMapping1.setAccountStore(dir1)
        accountStoreMapping1.setApplication(app)
        accountStoreMapping1 = app.createAccountStoreMapping(accountStoreMapping1)

        AccountStoreMapping accountStoreMapping2 = client.instantiate(AccountStoreMapping)
        accountStoreMapping2.setAccountStore(dir2)
        accountStoreMapping2.setApplication(app)
        accountStoreMapping2 = app.createAccountStoreMapping(accountStoreMapping2)

        dir1.createAccount(acct)
        deleteOnTeardown(acct)

        //Account belongs to dir1, therefore login must succeed
        def request = new UsernamePasswordRequest(username, password, accountStoreMapping1.getAccountStore())
        def result = app.authenticateAccount(request)
        assertEquals(result.getAccount().getUsername(), acct.username)

        try {
            //Account does not belong to dir2, therefore login must fail
            request = new UsernamePasswordRequest(username, password, accountStoreMapping2.getAccountStore())
            app.authenticateAccount(request)
            fail("Should have thrown due to invalid username/password");
        } catch (Exception e) {
            assertEquals(e.getMessage(), "HTTP 400, Stormpath 7104 (http://docs.stormpath.com/errors/7104): Login attempt failed because there is no Account in the Application's associated Account Stores with the specified username or email.")
        }

        //No account store has been defined, therefore login must succeed
        request = new UsernamePasswordRequest(username, password)
        result = app.authenticateAccount(request)
        assertEquals(result.getAccount().getUsername(), acct.username)
    }

    //@since 1.0.beta
    @Test
    void testGetNonexistentGoogleAccount() {
        Directory dir = client.instantiate(Directory)
        dir.name = uniquify("Java SDK: ApplicationIT.testGetNonexistentGoogleAccount")
        GoogleProvider provider = client.instantiate(GoogleProvider.class);
        def clientId = uniquify("999999911111111")
        def clientSecret = uniquify("a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0")
        provider.setClientId(clientId).setClientSecret(clientSecret).setRedirectUri("https://www.myAppURL:8090/index.jsp");

        def createDirRequest = Directories.newCreateRequestFor(dir).
                forProvider(Providers.GOOGLE.builder()
                        .setClientId(clientId)
                        .setClientSecret(clientSecret)
                        .setRedirectUri("https://www.myAppURL:8090/index.jsp")
                        .build()
                ).build()

        dir = client.currentTenant.createDirectory(createDirRequest)
        deleteOnTeardown(dir)

        def app = createTempApp()
        app.addAccountStore(dir)

        ProviderAccountRequest request = Providers.GOOGLE.account().setCode("4/MZ-Z4Xr-V6K61-Y0CE-ifJlyIVwY.EqwqoikzZTUSaDn_5y0ZQNiQIAI2iwI").build();

        try {
            app.getAccount(request)
            fail("should have thrown")
        } catch (com.stormpath.sdk.resource.ResourceException e) {
            assertEquals(e.getStatus(), 400)
            assertEquals(e.getCode(), 7200)
            assertEquals(e.getDeveloperMessage(), "Stormpath was not able to complete the request to Google: this can be " +
                    "caused by either a bad Google directory configuration, or the provided account credentials are not " +
                    "valid. Google error message: 400 Bad Request")
        }
    }

    @Test
    void testGetApiKeyById() {

        def app = createTempApp()

        def account = createTestAccount(app)

        def apiKey = account.createApiKey()

        def appApiKey = app.getApiKey(apiKey.id)

        assertNotNull appApiKey
        assertEquals appApiKey, apiKey

    }

    @Test
    void testGetApiKeyByIdCacheDisabled() {

        def app = createTempApp()

        def account = createTestAccount(app)

        def apiKey = account.createApiKey()

        client = buildClient(false)

        app = client.dataStore.getResource(app.href, Application)

        def appApiKey = app.getApiKey(apiKey.id)

        assertNotNull appApiKey
        assertEquals appApiKey, apiKey

    }

    @Test
    void testGetApiKeyByIdWithOptions() {

        def app = createTempApp()

        def account = createTestAccount(app)

        def apiKey = account.createApiKey()

        def client = buildClient(false) // need to disable caching because the api key is cached without the options
        app = client.getResource(app.href, Application)
        def appApiKey = app.getApiKey(apiKey.id, ApiKeys.options().withAccount().withTenant())

        assertNotNull appApiKey
        assertEquals appApiKey.secret, apiKey.secret
        assertTrue(appApiKey.account.propertyNames.size() > 1) // testing expansion
        assertTrue(appApiKey.tenant.propertyNames.size() > 1) // testing expansion

    }


    //https://github.com/stormpath/stormpath-sdk-java/issues/62 caused this bug: https://github.com/stormpath/stormpath-sdk-java/issues/74.
    //which made this test to fail. This issued was fixed in version 1.0.
    @Test
    void testGetApiKeyByIdWithOptionsInCache() {

        def app = createTempApp()

        def account = createTestAccount(app)

        def apiKey = account.createApiKey()

        def client = buildClient()
        app = client.getResource(app.href, Application)
        def appApiKey = app.getApiKey(apiKey.id, ApiKeys.options().withAccount().withTenant())
        def appApiKey2 = app.getApiKey(apiKey.id, ApiKeys.options().withAccount().withTenant())

        assertNotNull appApiKey
        assertNotNull appApiKey2
        assertEquals appApiKey2.secret, appApiKey.secret
        assertTrue(appApiKey.account.propertyNames.size() > 1) // testing expansion on the object retrieved from the server
        assertTrue(appApiKey.tenant.propertyNames.size() > 1) // testing expansion on the object retrieved from the server

        assertEquals appApiKey2.account.propertyNames.size(), appApiKey.account.propertyNames.size() // comparing object retrieved from the server and object obtained from cache
        assertEquals appApiKey2.tenant.propertyNames.size(), appApiKey.tenant.propertyNames.size() // comparing object retrieved from the server and object obtained from cache

        def dataStore = (DefaultDataStore) client.dataStore

        // testing that the secret is encrypted in the cache
        def apiKeyCache = dataStore.cacheManager.getCache(ApiKey.name)
        assertNotNull apiKeyCache
        def apiKeyCacheValue = apiKeyCache.get(appApiKey2.href)
        assertNotNull apiKeyCacheValue
        assertNotEquals apiKeyCacheValue['secret'], appApiKey2.secret
        assertEquals decryptSecretFromCacheMap(apiKeyCacheValue), appApiKey2.secret

        // testing that the expansions made it to the cache
        def accountCache = dataStore.cacheManager.getCache(Account.name)
        assertNotNull accountCache
        def accountCacheValue = accountCache.get(appApiKey2.account.href)
        assertNotNull accountCacheValue
        assertEquals accountCacheValue['username'], appApiKey.account.username

        def tenantCache = dataStore.cacheManager.getCache(Tenant.name)
        assertNotNull tenantCache
        def tenantCacheValue = tenantCache.get(appApiKey2.tenant.href)
        assertNotNull tenantCacheValue
        assertEquals tenantCacheValue['key'], appApiKey.tenant.key


    }

    @Test
    void testCreateSsoRedirectUrl() {
        def app = createTempApp()

        def ssoRedirectUrlBuilder = app.newIdSiteUrlBuilder()

        def ssoURL = ssoRedirectUrlBuilder.setCallbackUri("https://mycallbackuri.com/path").setPath("/mypath").setState("anyState").build()

        assertNotNull ssoURL

        String[] ssoUrlPath = ssoURL.split("jwtRequest=")

        assertEquals 2, ssoUrlPath.length

        StringTokenizer tokenizer = new StringTokenizer(ssoUrlPath[1], ".")

        //Expected JWT token 'base64Header'.'base64JsonPayload'.'base64Signature'
        assertEquals tokenizer.countTokens(), 3

        def base64Header = tokenizer.nextToken()
        def base64JsonPayload = tokenizer.nextToken()
        def base64Signature = tokenizer.nextToken()


        def objectMapper = new ObjectMapper()

        assertTrue Base64.isBase64(base64Header)
        assertTrue Base64.isBase64(base64JsonPayload)
        assertTrue Base64.isBase64(base64Signature)

        byte[] decodedJsonPayload = Base64.decodeBase64(base64JsonPayload)

        def jsonPayload = objectMapper.readValue(decodedJsonPayload, Map)

        assertEquals jsonPayload.cb_uri, "https://mycallbackuri.com/path"
        assertEquals jsonPayload.iss, client.dataStore.apiKey.id
        assertEquals jsonPayload.sub, app.href
        assertEquals jsonPayload.state, "anyState"
        assertEquals jsonPayload.path, "/mypath"
    }

    // @since 1.0.RC3
    @Test
    void testCreateSsoLogout() {
        def app = createTempApp()
        def ssoRedirectUrlBuilder = app.newIdSiteUrlBuilder()

        def ssoURL = ssoRedirectUrlBuilder.setCallbackUri("https://mycallbackuri.com/path").forLogout().build()

        assertNotNull ssoURL

        String[] ssoUrlPath = ssoURL.split("jwtRequest=")

        assertEquals 2, ssoUrlPath.length

        StringTokenizer tokenizer = new StringTokenizer(ssoUrlPath[1], ".")

        //Expected JWT token 'base64Header'.'base64JsonPayload'.'base64Signature'
        assertEquals tokenizer.countTokens(), 3

        def base64Header = tokenizer.nextToken()
        def base64JsonPayload = tokenizer.nextToken()
        def base64Signature = tokenizer.nextToken()


        def objectMapper = new ObjectMapper()

        assertTrue Base64.isBase64(base64Header)
        assertTrue Base64.isBase64(base64JsonPayload)
        assertTrue Base64.isBase64(base64Signature)

        byte[] decodedJsonPayload = Base64.decodeBase64(base64JsonPayload)

        def jsonPayload = objectMapper.readValue(decodedJsonPayload, Map)

        assertTrue ssoURL.startsWith(ssoRedirectUrlBuilder.SSO_ENDPOINT + "/logout?jwtRequest=")
        assertEquals jsonPayload.cb_uri, "https://mycallbackuri.com/path"
        assertEquals jsonPayload.iss, client.dataStore.apiKey.id
        assertEquals jsonPayload.sub, app.href
    }

    def Account createTestAccount(Application app) {

        def email = 'deleteme@nowhere.com'

        Account account = client.instantiate(Account)
        account.givenName = 'John'
        account.surname = 'DELETEME'
        account.email =  email
        account.password = 'Changeme1!'

        app.createAccount(account)
        deleteOnTeardown(account)

        return  account
    }

    String decryptSecretFromCacheMap(Map cacheMap) {

        if (cacheMap == null || cacheMap.isEmpty() || !cacheMap.containsKey(ApiKeyCacheParameter.API_KEY_META_DATA.toString())) {
            return null
        }

        def apiKeyMetaData = cacheMap[ApiKeyCacheParameter.API_KEY_META_DATA.toString()]

        def salt = apiKeyMetaData[ApiKeyParameter.ENCRYPTION_KEY_SALT.getName()]
        def keySize = apiKeyMetaData[ApiKeyParameter.ENCRYPTION_KEY_SIZE.getName()]
        def iterations = apiKeyMetaData[ApiKeyParameter.ENCRYPTION_KEY_ITERATIONS.getName()]

        def encryptionService = encryptionServiceBuilder
                .setBase64Salt(salt.getBytes())
                .setKeySize(keySize)
                .setIterations(iterations)
                .setPassword(client.dataStore.apiKey.secret.toCharArray()).build()

        def secret = encryptionService.decryptBase64String(cacheMap['secret'])

        return secret
    }

    /**
     * Asserts <a href="https://github.com/stormpath/stormpath-sdk-java/issues/58">Issue 58</a>.
     * @since 1.0.RC
     */
    @Test
    void testCreateApplicationViaTenantActions() {
        Application app = client.instantiate(Application)
        app.name = uniquify("Java SDK: ApplicationIT.testCreateApplicationViaTenantActions")
        app = client.createApplication(app);
        deleteOnTeardown(app)
        assertNotNull app.href
    }

    /**
     * @since 1.0.RC
     */
    @Test
    void testCreateApplicationRequestViaTenantActions() {
        Application app = client.instantiate(Application)
        app.name = uniquify("Java SDK: ApplicationIT.testCreateApplicationRequestViaTenantActions")
        def request = Applications.newCreateRequestFor(app).build()
        app = client.createApplication(request)
        deleteOnTeardown(app)
        assertNotNull app.href
    }

    /**
     * @since 1.0.RC
     */
    @Test
    void testGetApplicationsViaTenantActions() {
        def appList = client.getApplications()
        assertNotNull appList.href
    }

    /**
     * @since 1.0.RC
     */
    @Test
    void testGetApplicationsWithMapViaTenantActions() {
        def map = new HashMap<String, Object>()
        def appList = client.getApplications(map)
        assertNotNull appList.href
    }

    /**
     * @since 1.0.RC
     */
    @Test(enabled = false) //ignoring because of sporadic Travis failures
    void testGetApplicationsWithAppCriteriaViaTenantActions() {
        def appCriteria = Applications.criteria()
        def appList = client.getApplications(appCriteria)
        assertNotNull appList.href
    }

    /**
     * @since 1.0.RC3
     */
    @Test
    void testGetApplicationsWithCustomData() {

        def app = createTempApp()
        app.getCustomData().put("someKey", "someValue")
        app.save()

        def appList = client.getApplications(Applications.where(Applications.name().eqIgnoreCase(app.getName())).withCustomData())

        def count = 0
        for (Application application : appList) {
            count++
            assertNotNull(application.getHref())
            assertEquals(application.getCustomData().size(), 4)
        }
        assertEquals(count, 1)
    }

    /**
     * @since 1.0.RC3
     */
    @Test(enabled = false) //ignoring because of sporadic Travis failures
    void testAddAccountStore_Dirs() {
        def count = 1
        while (count >= 0) { //re-trying once
            try {
                Directory dir = client.instantiate(Directory)
                dir.name = uniquify("Java SDK: ApplicationIT.testAddAccountStore_Dirs")
                dir.description = dir.name + "-Description"
                dir = client.currentTenant.createDirectory(dir);
                deleteOnTeardown(dir)

                def app = createTempApp()

                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 1)

                def retrievedAccountStoreMapping = app.addAccountStore(dir.name)
                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 2)
                assertEquals(retrievedAccountStoreMapping.accountStore.href, dir.href)
                retrievedAccountStoreMapping.delete()
                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 1)

                retrievedAccountStoreMapping = app.addAccountStore(dir.href)
                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 2)
                assertEquals(retrievedAccountStoreMapping.accountStore.href, dir.href)
                retrievedAccountStoreMapping.delete()
                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 1)

                retrievedAccountStoreMapping = app.addAccountStore(Directories.criteria().add(Directories.description().eqIgnoreCase(dir.description)))
                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 2)
                assertEquals(retrievedAccountStoreMapping.accountStore.href, dir.href)
                retrievedAccountStoreMapping.delete()
                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 1)

                //Non-existent
                retrievedAccountStoreMapping = app.addAccountStore("non-existent Dir")
                assertNull(retrievedAccountStoreMapping)

                retrievedAccountStoreMapping = app.addAccountStore(dir.name.substring(0, 10))
                assertNull(retrievedAccountStoreMapping)

                retrievedAccountStoreMapping = app.addAccountStore(dir.name + "XXX")
                assertNull(retrievedAccountStoreMapping)

                retrievedAccountStoreMapping = app.addAccountStore(dir.href + "XXX")
                assertNull(retrievedAccountStoreMapping)

                retrievedAccountStoreMapping = app.addAccountStore(Directories.criteria().add(Directories.description().eqIgnoreCase(dir.description + "XXX")))
                assertNull(retrievedAccountStoreMapping)
            } catch (Exception e) {
                if (!(e instanceof ResourceException)) {
                    //this is the known sporadic exception; we will fail if there is a different one
                    throw e;
                }
                count--
                if (count < 0) {
                    throw e
                }
                System.out.println("Test failed due to " + e.getMessage() + ".\nRetrying " + (count + 1) + " more time(s)")
                continue
            }
            break;  //no error, let's get out of the loop
        }
    }

    /**
     * @since 1.0.RC3
     */
    @Test(enabled = false) //ignoring because of sporadic Travis failures
    void testAddAccountStore_Groups() {
        def count = 1
        while (count >= 0) { //re-trying once
            try {
                Directory dir = client.instantiate(Directory)
                dir.name = uniquify("Java SDK: ApplicationIT.testAddAccountStore_Groups")
                dir.description = dir.name + "-Description"
                dir = client.currentTenant.createDirectory(dir);
                deleteOnTeardown(dir)

                Group group = client.instantiate(Group)
                group.name = uniquify("Java SDK: ApplicationIT.testAddAccountStore_Groups")
                group.description = group.name + "-Description"
                dir.createGroup(group)

                def app = createTempApp()

                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 1)

                def retrievedAccountStoreMapping = app.addAccountStore(group.name)
                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 2)
                assertEquals(retrievedAccountStoreMapping.accountStore.href, group.href)
                retrievedAccountStoreMapping.delete()
                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 1)

                retrievedAccountStoreMapping = app.addAccountStore(group.href)
                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 2)
                assertEquals(retrievedAccountStoreMapping.accountStore.href, group.href)
                retrievedAccountStoreMapping.delete()
                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 1)

                retrievedAccountStoreMapping = app.addAccountStore(Groups.criteria().add(Groups.description().eqIgnoreCase(group.description)))
                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 2)
                assertEquals(retrievedAccountStoreMapping.accountStore.href, group.href)
                retrievedAccountStoreMapping.delete()
                assertAccountStoreMappingListSize(app.getAccountStoreMappings(), 1)

                //Non-existent
                retrievedAccountStoreMapping = app.addAccountStore("non-existent Group")
                assertNull(retrievedAccountStoreMapping)

                retrievedAccountStoreMapping = app.addAccountStore(group.name.substring(0, 10))
                assertNull(retrievedAccountStoreMapping)

                retrievedAccountStoreMapping = app.addAccountStore(group.href + "XXX")
                assertNull(retrievedAccountStoreMapping)

                retrievedAccountStoreMapping = app.addAccountStore(group.name + "XXX")
                assertNull(retrievedAccountStoreMapping)

                retrievedAccountStoreMapping = app.addAccountStore(Groups.criteria().add(Groups.description().eqIgnoreCase(group.description + "XXX")))
                assertNull(retrievedAccountStoreMapping)
            } catch (Exception e) {
                if (!(e instanceof ResourceException)) {
                    //this is the known sporadic exception; we will fail if there is a different one
                    throw e;
                }
                count--
                if (count < 0) {
                    throw e
                }
                System.out.println("Test failed due to " + e.getMessage() + ".\nRetrying " + (count + 1) + " more time(s)")
                continue
            }
            break;  //no error, let's get out of the loop
        }
    }


    /**
     * @since 1.0.RC3
     */
    @Test(enabled = false, expectedExceptions = IllegalArgumentException)  //ignoring because of sporadic Travis failures
    void testAddAccountStore_DirAndGroupMatch() {

        Directory dir01 = client.instantiate(Directory)
        dir01.name = uniquify("Java SDK: ApplicationIT.testAddAccountStore_DirAndGroupMatch")
        dir01.description = dir01.name + "-Description"
        dir01 = client.currentTenant.createDirectory(dir01);
        deleteOnTeardown(dir01)

        Directory dir02 = client.instantiate(Directory)
        dir02.name = uniquify("Java SDK: ApplicationIT.testAddAccountStore_DirAndGroupMatch")
        dir02.description = dir02.name + "-Description"
        dir02 = client.currentTenant.createDirectory(dir02);
        deleteOnTeardown(dir02)

        Group group = client.instantiate(Group)
        group.name = dir02.name
        group.description = group.name + "-Description"
        dir01.createGroup(group)

        def app = createTempApp()

        app.addAccountStore(group.name)
    }

    /**
     * @since 1.0.RC3
     */
    @Test(expectedExceptions = IllegalArgumentException)
    void testAddAccountStore_MultipleDirCriteria() {

        Directory dir01 = client.instantiate(Directory)
        dir01.name = uniquify("Java SDK: ApplicationIT.testAddAccountStore_MultipleDirCriteria")
        dir01.description = dir01.name + "-Description"
        dir01 = client.currentTenant.createDirectory(dir01);
        deleteOnTeardown(dir01)

        Directory dir02 = client.instantiate(Directory)
        dir02.name = uniquify("Java SDK: ApplicationIT.testAddAccountStore_MultipleDirCriteria")
        dir02.description = dir02.name + "-Description"
        dir02 = client.currentTenant.createDirectory(dir02);
        deleteOnTeardown(dir02)

        def app = createTempApp()

        app.addAccountStore(Directories.criteria().add(Directories.name().containsIgnoreCase("testAddAccountStore_MultipleDirCriteria")))
    }

    /**
     * @since 1.0.RC3
     */
    @Test(expectedExceptions = IllegalArgumentException)
    void testAddAccountStore_MultipleGroupCriteria() {

        Directory dir01 = client.instantiate(Directory)
        dir01.name = uniquify("Java SDK: ApplicationIT.testAddAccountStore_MultipleGroupCriteria")
        dir01.description = dir01.name + "-Description"
        dir01 = client.currentTenant.createDirectory(dir01);
        deleteOnTeardown(dir01)

        Directory dir02 = client.instantiate(Directory)
        dir02.name = uniquify("Java SDK: ApplicationIT.testAddAccountStore_MultipleGroupCriteria")
        dir02.description = dir02.name + "-Description"
        dir02 = client.currentTenant.createDirectory(dir02);
        deleteOnTeardown(dir02)

        Group group01 = client.instantiate(Group)
        group01.name = uniquify("Java SDK: ApplicationIT.testAddAccountStore_MultipleGroupCriteria")
        group01.description = group01.name + "-Description"
        dir01.createGroup(group01)

        Group group02 = client.instantiate(Group)
        group02.name = uniquify("Java SDK: ApplicationIT.testAddAccountStore_MultipleGroupCriteria")
        group02.description = group02.name + "-Description"
        dir02.createGroup(group02)

        def app = createTempApp()

        app.addAccountStore(Groups.criteria().add(Groups.name().containsIgnoreCase("testAddAccountStore_MultipleGroupCriteria")))
    }

    private assertAccountStoreMappingListSize(AccountStoreMappingList accountStoreMappings, int expectedSize) {
        int qty = 0;
        for(AccountStoreMapping accountStoreMapping : accountStoreMappings) {
            qty++;
        }
        assertEquals(qty, expectedSize)
    }

}
