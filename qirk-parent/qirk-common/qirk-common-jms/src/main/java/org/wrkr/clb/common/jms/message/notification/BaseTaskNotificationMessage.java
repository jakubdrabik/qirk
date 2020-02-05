/*
 * This file is part of the Java API to Qirk.
 * Copyright (C) 2020 Memfis Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.wrkr.clb.common.jms.message.notification;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class BaseTaskNotificationMessage extends BaseNotificationMessage {

    @JsonIgnore
    public static final String ORGANIZATION_UI_ID = "organization_ui_id";
    @JsonIgnore
    public static final String PROJECT_UI_ID = "project_ui_id";

    @JsonProperty(value = ORGANIZATION_UI_ID)
    public String organizationUiId;
    @JsonProperty(value = PROJECT_UI_ID)
    public String projectUiId;

    protected BaseTaskNotificationMessage(String type) {
        super(type);
    }

    public BaseTaskNotificationMessage(String type, Collection<Long> subscriberIds, Collection<String> subscriberEmails) {
        super(type, subscriberIds, subscriberEmails);
    }
}