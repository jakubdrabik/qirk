/*
 * This file is part of the Java API to Qirk.
 * Copyright (C) 2020 Memfis LLC, Russia
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
package org.wrkr.clb.services.dto.project;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Tuple;

import org.wrkr.clb.common.util.datetime.DateTimeUtils;
import org.wrkr.clb.model.project.Project;
import org.wrkr.clb.model.project.ProjectMember;
import org.wrkr.clb.services.dto.IdDTO;
import org.wrkr.clb.services.dto.user.PublicUserDTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProjectMemberReadDTO extends IdDTO {

    @JsonInclude(Include.NON_NULL)
    public PublicUserDTO user;

    @JsonInclude(Include.NON_NULL)
    public ProjectNameAndUiIdDTO project;

    @JsonProperty(value = "write_allowed")
    @JsonInclude(Include.NON_NULL)
    public Boolean writeAllowed;

    @JsonInclude(Include.NON_NULL)
    public Boolean manager;

    @JsonProperty(value = "hired_at")
    @JsonInclude(Include.NON_NULL)
    public String hiredAt;

    @JsonProperty(value = "fired_at")
    @JsonInclude(Include.NON_NULL)
    public String firedAt;

    @JsonInclude(Include.NON_NULL)
    public Boolean fired;

    public static ProjectMemberReadDTO fromEntity(ProjectMember member, boolean includeUser, boolean includeProject,
            boolean includePermissions, boolean includeTimestamps) {
        ProjectMemberReadDTO dto = new ProjectMemberReadDTO();

        dto.id = member.getId();

        if (includeUser) {
            dto.user = PublicUserDTO.fromEntity(member.getUser());
        }

        if (includeProject) {
            dto.project = ProjectNameAndUiIdDTO.fromEntity(member.getProject());
        }

        if (includePermissions) {
            dto.writeAllowed = member.isWriteAllowed();
            dto.manager = member.isManager();
        }

        if (includeTimestamps) {
            dto.hiredAt = member.getHiredAt().format(DateTimeUtils.WEB_DATETIME_FORMATTER);
            if (member.getFiredAt() != null) {
                dto.firedAt = member.getFiredAt().format(DateTimeUtils.WEB_DATETIME_FORMATTER);
            }
            dto.fired = member.isFired();
        }

        return dto;
    }

    public static ProjectMemberReadDTO fromEntityWithUserAndPermissions(ProjectMember member) {
        return fromEntity(member, true, false, true, false);
    }

    public static ProjectMemberReadDTO fromEntityWithUserAndProjectAndPermissions(ProjectMember member) {
        return fromEntity(member, true, true, true, false);
    }

    public static ProjectMemberReadDTO fromEntityWithTimestamps(ProjectMember member) {
        return fromEntity(member, false, false, false, true);
    }

    public static List<ProjectMemberReadDTO> fromEntitiesForProject(List<ProjectMember> memberList) {
        List<ProjectMemberReadDTO> dtoList = new ArrayList<ProjectMemberReadDTO>(memberList.size());
        for (ProjectMember member : memberList) {
            dtoList.add(fromEntityWithUserAndPermissions(member));
        }
        return dtoList;
    }

    public static List<ProjectMemberReadDTO> fromProjectMemberWithProjectTuples(List<Tuple> memberList) {
        List<ProjectMemberReadDTO> dtoList = new ArrayList<ProjectMemberReadDTO>();
        for (Tuple member : memberList) {
            ProjectMemberReadDTO dto = fromEntityWithTimestamps(member.get(0, ProjectMember.class));
            dto.project = ProjectNameAndUiIdDTO.fromEntity(member.get(1, Project.class));
            dtoList.add(dto);
        }
        return dtoList;
    }
}
