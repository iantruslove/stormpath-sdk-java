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
package com.stormpath.sdk.impl.oauth.authc;

import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.account.AccountStatus;
import com.stormpath.sdk.api.ApiKey;
import com.stormpath.sdk.api.ApiKeyStatus;
import com.stormpath.sdk.application.Application;
import com.stormpath.sdk.error.authc.AccessTokenOauthException;
import com.stormpath.sdk.error.jwt.InvalidJwtException;
import com.stormpath.sdk.impl.api.DefaultApiKeyOptions;
import com.stormpath.sdk.impl.ds.InternalDataStore;
import com.stormpath.sdk.impl.error.ApiAuthenticationExceptionFactory;
import com.stormpath.sdk.impl.jwt.JwtSignatureValidator;
import com.stormpath.sdk.impl.jwt.JwtWrapper;
import com.stormpath.sdk.lang.Assert;
import com.stormpath.sdk.lang.Strings;
import com.stormpath.sdk.oauth.OauthAuthenticationResult;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import static com.stormpath.sdk.error.authc.AccessTokenOauthException.*;
import static org.apache.oltu.oauth2.common.OAuth.*;

/** @since 1.0.RC */
public class ResourceRequestAuthenticator {

    public final static String SCOPE_SEPARATOR_CHAR = " ";

    private final InternalDataStore dataStore;

    private final JwtSignatureValidator jwtSignatureValidator;

    public ResourceRequestAuthenticator(InternalDataStore dataStore) {
        Assert.notNull(dataStore, "datastore cannot be null or empty.");
        this.dataStore = dataStore;
        this.jwtSignatureValidator = new JwtSignatureValidator(dataStore.getApiKey());
    }

    public OauthAuthenticationResult authenticate(Application application, ResourceAuthenticationRequest request) {

        JwtWrapper jwtWrapper;

        try {
            jwtWrapper = new JwtWrapper(request.getAccessToken());
            jwtSignatureValidator.validate(jwtWrapper);
        } catch (OAuthSystemException e) {
            throw ApiAuthenticationExceptionFactory
                .newOauthException(AccessTokenOauthException.class, INVALID_ACCESS_TOKEN);
        } catch (InvalidJwtException e) {
            throw ApiAuthenticationExceptionFactory
                .newOauthException(AccessTokenOauthException.class, INVALID_ACCESS_TOKEN);
        }

        Map jsonMap = jwtWrapper.getJsonPayloadAsMap();

        //Number createdTimestamp = getRequiredValue(jsonMap, AccessTokenRequestAuthenticator.ACCESS_TOKEN_CREATION_TIMESTAMP_FIELD_NAME);

        Number expirationTimestamp =
            getRequiredValue(jsonMap, AccessTokenRequestAuthenticator.ACCESS_TOKEN_EXPIRATION_TIMESTAMP_FIELD_NAME);

        assertTokenNotExpired(/*createdTimestamp.longValue(), */expirationTimestamp.longValue());

        String apiKeyId = getRequiredValue(jsonMap, AccessTokenRequestAuthenticator.ACCESS_TOKEN_SUBJECT_FIELD_NAME);

        //Retrieve the ApiKey that owns this
        ApiKey apiKey = getTokenApiKey(application, apiKeyId);

        String grantedScopes = getOptionalValue(jsonMap, OAUTH_SCOPE);

        Set<String> scope;
        if (Strings.hasText(grantedScopes)) {
            scope = new HashSet<String>();
            for (StringTokenizer scopeTokenizer = new StringTokenizer(grantedScopes, SCOPE_SEPARATOR_CHAR);
                 scopeTokenizer.hasMoreElements(); ) {
                String scopeValue = scopeTokenizer.nextToken();
                scope.add(scopeValue);
            }
        } else {
            scope = Collections.emptySet();
        }

        return new DefaultOauthAuthenticationResult(dataStore, apiKey, scope);
    }

    private void assertTokenNotExpired(long expirationTimestampAsSecondsSinceEpoch) {

        long nowAsSecondsSinceEpoch = System.currentTimeMillis() / 1000;

        if (nowAsSecondsSinceEpoch >= expirationTimestampAsSecondsSinceEpoch) {
            throw ApiAuthenticationExceptionFactory
                .newOauthException(AccessTokenOauthException.class, EXPIRED_ACCESS_TOKEN);
        }
    }

    /**
     * Retrieves the {@link ApiKey} instance pointed by this {@code apiKeyId} and accessible from the {@code
     * application}
     * <p/>
     * The ApiKey is retrieved from the {@link Application} passed as argument.
     * <p/>
     * This method asserts that the ApiKey retrieved status is {@link ApiKeyStatus#ENABLED} and also that the status of
     * the account owner is {@link AccountStatus#ENABLED}
     *
     * @param application - The application that is making the assertion.
     * @param apiKeyId    - The id of the {@link ApiKey} embedded in the access token.
     */
    private ApiKey getTokenApiKey(Application application, String apiKeyId) {

        ApiKey apiKey = application.getApiKey(apiKeyId, new DefaultApiKeyOptions().withAccount());

        if (apiKey == null || apiKey.getStatus() == ApiKeyStatus.DISABLED) {
            throw ApiAuthenticationExceptionFactory.newOauthException(AccessTokenOauthException.class, INVALID_CLIENT);
        }

        Account account = apiKey.getAccount();

        if (account.getStatus() != AccountStatus.ENABLED) {
            throw ApiAuthenticationExceptionFactory.newOauthException(AccessTokenOauthException.class, INVALID_CLIENT);
        }

        return apiKey;
    }

    private <T> T getRequiredValue(Map jsonMap, String parameterName) {

        Object object = jsonMap.get(parameterName);

        Assert.notNull(object, "required jwt parameter is missing or null.");

        return (T) object;
    }

    private <T> T getOptionalValue(Map jsonMap, String parameterName) {

        Object object = jsonMap.get(parameterName);

        if (object == null) {
            return null;
        }

        return (T) object;
    }
}
