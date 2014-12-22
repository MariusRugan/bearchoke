/*
 * Copyright 2014 the original author or authors.
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

package com.bearchoke.platform.user;

import com.bearchoke.platform.api.user.AuthenticateUserCommand;
import com.bearchoke.platform.api.user.CreateUserCommand;
import com.bearchoke.platform.api.user.RegisterUserCommand;
import com.bearchoke.platform.api.user.RoleIdentifier;
import com.bearchoke.platform.api.user.UserAccount;
import com.bearchoke.platform.api.user.UserIdentifier;
import com.bearchoke.platform.platform.base.PlatformConstants;
import com.bearchoke.platform.user.document.User;
import com.bearchoke.platform.user.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.repository.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Created by Bjorn Harvold
 * Date: 12/8/14
 * Time: 7:21 PM
 * Responsibility:
 */
@Component
@Slf4j
public class UserCommandHandler {
    @Qualifier("userAggregateRepository")
    private final Repository<UserAggregate> userAggregateRepository;

    @Qualifier("userRepository")
    private final UserRepository userRepository;

    @Autowired
    public UserCommandHandler(Repository<UserAggregate> userAggregateRepository, UserRepository userRepository) {
        this.userAggregateRepository = userAggregateRepository;
        this.userRepository = userRepository;
    }

    @CommandHandler
    public UserIdentifier handleRegisterUserAggregate(RegisterUserCommand command) {
        if (log.isDebugEnabled()) {
            log.debug("Handling: " + command.getClass().getSimpleName());
        }

        UserIdentifier id = command.getUserId();
        UserAggregate u = new UserAggregate(
                id,
                command.getUsername(),
                command.getPassword(),
                command.getEmail(),
                command.getFirstName(),
                command.getLastName(),
                Arrays.asList(PlatformConstants.DEFAULT_USER_ROLE)
        );

        // persist user aggregate
        userAggregateRepository.add(u);

        return id;
    }

    @CommandHandler
    public UserIdentifier handleCreateUserAggregate(CreateUserCommand command) {
        if (log.isDebugEnabled()) {
            log.debug("Handling: " + command.getClass().getSimpleName());
        }

        UserIdentifier id = command.getUserId();
        UserAggregate u = new UserAggregate(
                id,
                command.getUsername(),
                command.getPassword(),
                command.getEmail(),
                command.getFirstName(),
                command.getLastName(),
                command.getRoles()
        );

        // persist user aggregate
        userAggregateRepository.add(u);

        return id;
    }

    @CommandHandler
    public UserAccount handleAuthenticateUser(AuthenticateUserCommand command) {
        if (log.isDebugEnabled()) {
            log.debug("Handling: " + command.getClass().getSimpleName());
        }
        User user = userRepository.findUserByUsername(command.getUsername());

        if (user == null) {
            return null;
        }

        boolean success = onUser(user.getUserIdentifier()).authenticate(command.getPassword());

        if (log.isDebugEnabled()) {
            log.debug("Authentication successful: " + success);
        }

        return success ? user : null;
    }

    private UserAggregate onUser(String userId) {
        UserAggregate ua = userAggregateRepository.load(new UserIdentifier(userId));

        if (log.isDebugEnabled()) {
            log.debug("Found user aggregate: " + ua);
        }

        return ua;
    }
}
