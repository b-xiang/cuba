/*
 * Copyright (c) 2008-2016 Haulmont.
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
 *
 */
package com.haulmont.cuba.security.listener;

import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.PersistenceTools;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.PasswordEncryption;
import com.haulmont.cuba.core.listener.AfterDeleteEntityListener;
import com.haulmont.cuba.core.listener.BeforeInsertEntityListener;
import com.haulmont.cuba.core.listener.BeforeUpdateEntityListener;
import com.haulmont.cuba.security.entity.Role;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.entity.UserRole;
import com.haulmont.cuba.security.events.UserInvalidationEvent;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@Component("cuba_UserEntityListener")
public class UserEntityListener implements BeforeInsertEntityListener<User>, BeforeUpdateEntityListener<User>,
        AfterDeleteEntityListener<User> {

    @Inject
    protected Events events;

    @Inject
    protected PersistenceTools persistenceTools;

    @Inject
    protected Metadata metadata;

    @Inject
    protected PasswordEncryption passwordEncryption;

    @Override
    public void onBeforeInsert(User entity, EntityManager entityManager) {
        addDefaultRoles(entity, entityManager);
        updatePasswordEncryption(entity);
        updateLoginLowerCase(entity);
    }

    protected void addDefaultRoles(User user, EntityManager entityManager) {
        if (user.isDisabledDefaultRoles())
            return;

        List<Role> defaultRoles = entityManager.createQuery(
                "select r from sec$Role r where r.defaultRole = true", Role.class)
                .getResultList();

        if (user.getUserRoles() == null)
            user.setUserRoles(new ArrayList<>());

        for (Role defaultRole : defaultRoles) {
            if (user.getUserRoles().stream().noneMatch(userRole -> userRole.getRole().equals(defaultRole))) {
                UserRole userRole = metadata.create(UserRole.class);
                userRole.setUser(user);
                userRole.setRole(defaultRole);

                entityManager.persist(userRole);
                user.getUserRoles().add(userRole);
            }
        }
    }

    @Override
    public void onBeforeUpdate(User entity, EntityManager entityManager) {
        updateLoginLowerCase(entity);

        //noinspection ConstantConditions
        if (persistenceTools.getDirtyFields(entity).contains("active")
                && BooleanUtils.isTrue((Boolean) persistenceTools.getOldValue(entity, "active"))) {
            events.publish(new UserInvalidationEvent(entity));
        }
    }

    protected void updateLoginLowerCase(User user) {
        user.setLoginLowerCase(user.getLogin() != null ? user.getLogin().toLowerCase() : null);
    }

    protected void updatePasswordEncryption(User user) {
        if (user.getPasswordEncryption() == null) {
            user.setPasswordEncryption(passwordEncryption.getHashMethod());
        }
    }

    @Override
    public void onAfterDelete(User entity, Connection connection) {
        events.publish(new UserInvalidationEvent(entity));
    }
}