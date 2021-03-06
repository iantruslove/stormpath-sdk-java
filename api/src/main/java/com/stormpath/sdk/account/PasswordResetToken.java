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
package com.stormpath.sdk.account;

import com.stormpath.sdk.directory.AccountStore;
import com.stormpath.sdk.resource.Resource;

/**
 * @since 0.2
 */
public interface PasswordResetToken extends Resource {

    String getEmail();

    PasswordResetToken setEmail(String email);

    Account getAccount();

    PasswordResetToken setAccountStore(AccountStore accountStore);

    /**
     * Setter for the new password that will be instantly applied if the reset token is correctly validated.
     *
     * @param password the new password that will be applied if the reset token is correctly validated.
     * @since 1.0.RC
     */
    PasswordResetToken setPassword(String password);
}
