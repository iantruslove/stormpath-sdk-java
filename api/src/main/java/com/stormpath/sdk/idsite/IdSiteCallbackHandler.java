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
package com.stormpath.sdk.idsite;

/**
 * Handles HTTP replies sent from your ID Site to your application's {@code callbackUri}, for example,
 * {@code https://awesomeapp.com/id}, and returns an {@link AccountResult}.
 *
 * <p>Usage example is documented in
 * {@code application.}{@link com.stormpath.sdk.application.Application#newIdSiteUrlBuilder() newIdSiteUrlBuilder()}.
 * </p>
 *
 * @see com.stormpath.sdk.application.Application#newIdSiteCallbackHandler(Object)
 * @see #setNonceStore(NonceStore)
 * @see #getAccountResult()
 * @since 1.0.RC2
 */
public interface IdSiteCallbackHandler {

    /**
     * Sets a {@code NonceStore} that will retain ID Site message identifiers as
     * <a href="http://en.wikipedia.org/wiki/Cryptographic_nonce">nonces</a>, eliminating
     * <a href="http://en.wikipedia.org/wiki/Replay_attack">Replay Attacks</a>.  This ensures any ID Site messages
     * cannot be intercepted and used again later.
     *
     * <p>Because this ensures strong security, a {@code NonceStore} is enabled by default <b>IF</b>
     * you have caching enabled for the SDK: a cache region will be used to store nonces over time, and those nonces
     * will be used to assert that ID Site replies are not used more than once.</p>
     *
     * <h5>Default NonceStore</h5>
     *
     * <p>As mentioned above, a Cache-based {@code NonceStore} is enabled by default if you have caching enabled in the
     * SDK.  Because nonces are stored in a cache region, it is very important to ensure that the nonce store cache
     * region has an entry TTL <em>longer</em> than the response message valid life span.  For Stormpath, response
     * messages are valid for 1 minute, so your default cache region settings must use a TTL longer than 1 minute
     * (most caching system defaults are ~ 30 minutes or an hour, so odds are high that you're 'ok').</p>
     *
     * <h5>Custom Nonce Store</h5>
     *
     * <p>If you have not enabled caching in the SDK, or you don't want to use your SDK cache as a {@code NonceStore},
     * you  can specify a custom instance via this method, and all ID Site reply identifiers will be stored there to
     * prevent reuse, but note: your custom {@code NonceStore} implementation <em>MUST</em> support the notion of a
     * TTL (Time-to-Live) and automatically evict entries older than the max age lifespan (again, 1 minute).</p>
     *
     * <p>If your {@code NonceStore} implementation does not support TTL auto-eviction, your store will fill up
     * indefinitely, likely causing storage errors.</p>
     *
     * @param nonceStore the {@link NonceStore} implementation to use during the process to execute this request.
     * @throws IllegalArgumentException when the {@code nonceStore} argument is {@code null}.
     * @return this instance for method chaining
     */
    IdSiteCallbackHandler setNonceStore(NonceStore nonceStore);

    /**
     * Actually processes the request and returns an {@code AccountResult} object that reflects the account that
     * logged in or registered.
     * <p/>
     * During the execution of this method, the {@link IdSiteResultListener result listener} (if any) will be notified
     * about the actual operation of the ID Site invocation: registration, authentication or logout.
     *
     * @return the resolved identity in the form of an {@link AccountResult}
     */
    AccountResult getAccountResult();

    /**
     * Sets the {@link IdSiteResultListener listener} that will be notified about the actual operation of the Id Site invocation:
     * registration, authentication or logout.
     * <p/>
     * The listener must be set before the method {@link #getAccountResult()} is invoked since this the actual operation that
     * will trigger the notification to the specified listener.
     * <p/>
     * Example:
     * <pre
     * <code>
     * IdSiteCallbackHandler callbackHandler = application.newIdSiteCallbackHandler(request);
     * callbackHandler.setResultListener(new IdSiteResultListener() {
     *      {@literal@}Override
     *      public void onRegistered(RegistrationResult result) {
     *          System.out.println("Successful Id Site registration");
     *      }
     *
     *      {@literal@}Override
     *      public void onAuthenticated(AuthenticationResult result) {
     *          System.out.println("Successful Id Site authentication");
     *      }
     *
     *      {@literal@}Override
     *      public void onLogout(LogoutResult result) {
     *          System.out.println("Successful Id Site logout");
     *      }
     * });
     * Account account = callbackHandler.getAccountResult().getAccount();
     * </code>
     * </pre
     *
     * @param resultListener the {@link IdSiteResultListener} that will be notified about the actual operation of the ID Site
     *                       invocation: registration, authentication or logout. If <code>resultListener<code/> is null, no notification
     *                       will be sent.
     * @return this instance for method chaining
     * @see IdSiteResultListener
     * @since 1.0.RC3
     */
    IdSiteCallbackHandler setResultListener(IdSiteResultListener resultListener);

}
