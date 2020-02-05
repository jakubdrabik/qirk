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
package org.wrkr.clb.services.project.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Tuple;

import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.wrkr.clb.common.util.datetime.DateTimeUtils;
import org.wrkr.clb.model.organization.OrganizationMember;
import org.wrkr.clb.model.project.Project;
import org.wrkr.clb.model.project.ProjectMember;
import org.wrkr.clb.model.user.User;
import org.wrkr.clb.repo.organization.JDBCOrganizationMemberRepo;
import org.wrkr.clb.repo.organization.OrganizationMemberRepo;
import org.wrkr.clb.repo.project.JDBCProjectMemberRepo;
import org.wrkr.clb.repo.project.JDBCProjectRepo;
import org.wrkr.clb.repo.project.ProjectMemberRepo;
import org.wrkr.clb.repo.project.ProjectRepo;
import org.wrkr.clb.repo.project.task.TaskSubscriberRepo;
import org.wrkr.clb.services.api.elasticsearch.ElasticsearchUserService;
import org.wrkr.clb.services.dto.IdOrUiIdDTO;
import org.wrkr.clb.services.dto.organization.OrganizationMemberUserDTO;
import org.wrkr.clb.services.dto.project.ProjectMemberDTO;
import org.wrkr.clb.services.dto.project.ProjectMemberListDTO;
import org.wrkr.clb.services.dto.project.ProjectMemberReadDTO;
import org.wrkr.clb.services.project.ProjectMemberService;
import org.wrkr.clb.services.security.ProjectSecurityService;
import org.wrkr.clb.services.security.SecurityService;
import org.wrkr.clb.services.user.UserFavoriteService;
import org.wrkr.clb.services.util.exception.ApplicationException;
import org.wrkr.clb.services.util.exception.NotFoundException;

@Validated
@Service
public class DefaultProjectMemberService implements ProjectMemberService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultProjectMemberService.class);

    private static final long PROJECT_MEMBERSHIP_MINIMUM_DURATION_MILLIS = 24 * 60 * 60 * 1000; // 1 day

    @Autowired
    private ProjectMemberRepo projectMemberRepo;

    @Autowired
    private JDBCProjectMemberRepo jdbcProjectMemberRepo;

    @Autowired
    private ProjectRepo projectRepo;

    @Autowired
    private JDBCProjectRepo jdbcProjectRepo;

    @Autowired
    private OrganizationMemberRepo orgMemberRepo;

    @Autowired
    private JDBCOrganizationMemberRepo jdbcOrgMemberRepo;

    @Autowired
    private TaskSubscriberRepo taskSubscriberRepo;

    @Autowired
    private UserFavoriteService userFavoriteService;

    @Autowired
    private ElasticsearchUserService elasticsearchService;

    @Autowired
    private ProjectSecurityService securityService;

    @Autowired
    private SecurityService authnSecurityService;

    @Deprecated
    @SuppressWarnings("unused")
    private void freezeProjectIfCountExceededMinMembersToCharge(Project project) {
        if (project.getOrganization().isFrozen() && !project.isFrozen()) {
            long projectMembersCount = jdbcOrgMemberRepo
                    .countNotFiredManagersOrProjectMembersByOrganizationIdAndProjectId(
                            project.getOrganization(), project);
            if (projectMembersCount >= Project.MIN_PRIVATE_PROJECT_MEMBERS_TO_CHARGE) {
                project.setFrozen(true);
                jdbcProjectRepo.updateFrozen(project);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ProjectMember create(Project project, User user, ProjectMemberDTO projectMemberDTO) {
        ProjectMember member = projectMemberRepo.getNotFiredByUserAndProject(user, project);
        if (member != null) {
            return member;
        }

        member = new ProjectMember();
        member.setProject(project);
        member.setOrganizationMember(organizationMember);
        member.setUser(organizationMember.getUser());

        member.setWriteAllowed(projectMemberDTO.writeAllowed || projectMemberDTO.manager);
        member.setManager(projectMemberDTO.manager);
        member.setHiredAt(DateTimeUtils.now());

        projectMemberRepo.persist(member);

        // uncomment when payment is on
        // freezeProject(project);

        Long userId = member.getUser().getId();
        try {
            elasticsearchService.addProject(userId, member.getProject().getId());
        } catch (Exception e) {
            LOG.error("Could not add project for user " + userId + " in elasticsearch", e);
        }

        return member;
    }

    @Override
    @Transactional(value = "jpaTransactionManager", rollbackFor = Throwable.class)
    public ProjectMemberReadDTO create(User currentUser, ProjectMemberDTO projectMemberDTO)
            throws ApplicationException {
        // security start
        securityService.authzCanModifyProjectMembers(currentUser, projectMemberDTO.projectId);
        // security finish

        Project project = projectRepo.get(projectMemberDTO.projectId);
        if (project == null) {
            throw new NotFoundException("Project");
        }

        OrganizationMember organizationMember = null;
        if (ProjectMemberDTO.CURRENT_USER_ORGANIZATION_MEMBER_ID.equals(projectMemberDTO.organizationMemberId)) {
            organizationMember = orgMemberRepo.getNotFiredByUserAndOrganization(currentUser,
                    project.getOrganization());
        } else {
            organizationMember = orgMemberRepo
                    .getNotFiredByIdAndOrganizationAndFetchUser(projectMemberDTO.organizationMemberId,
                            project.getOrganization());
        }
        if (organizationMember == null) {
            throw new NotFoundException("Organization member");
        }

        ProjectMember projectMember = create(project, organizationMember, projectMemberDTO);
        return ProjectMemberReadDTO.fromEntityWithUserAndProjectAndPermissions(projectMember);
    }

    @Override
    @Transactional(value = "jpaTransactionManager", rollbackFor = Throwable.class)
    public List<ProjectMemberReadDTO> createBatch(User currentUser, ProjectMemberListDTO projectMemberListDTO)
            throws ApplicationException {
        // security start
        securityService.authzCanModifyProjectMembers(currentUser, projectMemberListDTO.projectId);
        // security finish

        Project project = projectRepo.get(projectMemberListDTO.projectId);
        if (project == null) {
            throw new NotFoundException("Project");
        }

        List<ProjectMemberReadDTO> readDTOList = new ArrayList<ProjectMemberReadDTO>();
        for (ProjectMemberDTO projectMemberDTO : projectMemberListDTO.members) {
            OrganizationMember organizationMember = orgMemberRepo
                    .getNotFiredByIdAndOrganizationAndFetchUser(projectMemberDTO.organizationMemberId,
                            project.getOrganization());
            if (organizationMember == null) {
                throw new NotFoundException("Organization member");
            }

            ProjectMember projectMember = create(project, organizationMember, projectMemberDTO);
            readDTOList.add(ProjectMemberReadDTO.fromEntityWithUserAndProjectAndPermissions(projectMember));
        }
        return readDTOList;
    }

    @Override
    @Transactional(value = "jpaTransactionManager", rollbackFor = Throwable.class, readOnly = true)
    public ProjectMemberReadDTO get(User currentUser, Long id) throws ApplicationException {
        // security start
        securityService.authzCanReadProjectMember(currentUser, id);
        // security finish

        ProjectMember member = projectMemberRepo.getNotFiredAndFetchUserAndProject(id);
        if (member == null) {
            throw new NotFoundException("Project member");
        }
        return ProjectMemberReadDTO.fromEntityWithUserAndProjectAndPermissions(member);
    }

    @Override
    @Transactional(value = "jpaTransactionManager", rollbackFor = Throwable.class)
    public ProjectMemberReadDTO update(User currentUser, ProjectMemberDTO projectMemberDTO)
            throws ApplicationException {
        // security start
        securityService.authzCanModifyProjectMember(currentUser, projectMemberDTO.id);
        // security finish

        ProjectMember member = projectMemberRepo.getNotFiredAndFetchUserAndProject(projectMemberDTO.id);
        if (member == null) {
            throw new NotFoundException("Project member");
        }

        member.setWriteAllowed(projectMemberDTO.writeAllowed || projectMemberDTO.manager);
        member.setManager(projectMemberDTO.manager);

        member = projectMemberRepo.merge(member);
        return ProjectMemberReadDTO.fromEntityWithUserAndProjectAndPermissions(member);
    }

    @Override
    @Transactional(value = "jpaTransactionManager", rollbackFor = Throwable.class, readOnly = true)
    public List<ProjectMemberReadDTO> listByProjectId(User currentUser, Long projectId) {
        // security start
        securityService.authzCanReadProjectMembers(currentUser, projectId);
        // security finish

        List<ProjectMember> memberList = projectMemberRepo.listNotFiredByProjectIdAndFetchUser(projectId);
        return ProjectMemberReadDTO.fromEntitiesForProject(memberList);
    }

    @Override
    @Transactional(value = "jpaTransactionManager", rollbackFor = Throwable.class, readOnly = true)
    public List<ProjectMemberReadDTO> listByProjectUiId(User currentUser, String projectUiId) {
        // security start
        securityService.authzCanReadProjectMembers(currentUser, projectUiId);
        // security finish

        List<ProjectMember> memberList = projectMemberRepo.listNotFiredByProjectUiIdAndFetchUser(projectUiId);
        return ProjectMemberReadDTO.fromEntitiesForProject(memberList);
    }

    @Override
    @Transactional(value = "jpaTransactionManager", rollbackFor = Throwable.class, readOnly = true)
    public List<ProjectMemberReadDTO> listByUser(User currentUser, Long userId) {
        // security start
        authnSecurityService.authzCanReadUserProfile(currentUser, userId);
        // security finish

        List<Tuple> memberList = projectMemberRepo.listPublicByUserIdAndFetchProject(userId);
        return ProjectMemberReadDTO.fromProjectMemberWithProjectTuples(memberList);
    }

    @Deprecated
    @SuppressWarnings("unused")
    private void unfreezeProjectIfCountBecameLessThanMinMembersToCharge(Project project) {
        if (project.getOrganization().isFrozen() && project.isFrozen()) {
            long projectMembersCount = jdbcOrgMemberRepo
                    .countNotFiredManagersOrProjectMembersByOrganizationIdAndProjectId(
                            project.getOrganization(), project);
            if (projectMembersCount < Project.MIN_PRIVATE_PROJECT_MEMBERS_TO_CHARGE) {
                project.setFrozen(false);
                jdbcProjectRepo.updateFrozen(project);
            }
        }
    }

    private void delete(ProjectMember member, boolean deleteUserFavorite) {
        long now = System.currentTimeMillis();
        if (now - member.getHiredAt().toInstant().toEpochMilli() < PROJECT_MEMBERSHIP_MINIMUM_DURATION_MILLIS) {
            jdbcProjectMemberRepo.deleteNotFiredById(member.getId());
        } else {
            jdbcProjectMemberRepo.setFiredTrueAndFiredAtNowForNotFiredById(member.getId());
        }

        Long userId = member.getUserId();
        Project project = member.getProject();
        if (project.isPrivate()) {
            taskSubscriberRepo.deleteByUserIdAndProjectId(userId, project.getId());
        }

        // not used yet
        if (deleteUserFavorite) {
            if (project.isPrivate() && !member.getOrganizationMember().isManager()) {
                userFavoriteService.deleteByUserIdAndProject(member.getUserId(), member.getProject());
            }
        }

        // uncomment when payment is on
        // unfreezeProjectIfCountBecameLessThanMinMembersToCharge(project);

        try {
            elasticsearchService.removeProject(userId, project.getId());
        } catch (Exception e) {
            LOG.error("Could not remove project for user " + userId + " in elasticsearch", e);
        }
    }

    private void delete(ProjectMember member) {
        delete(member, false);
    }

    @Override
    @Transactional(value = "jpaTransactionManager", rollbackFor = Throwable.class)
    public void delete(User currentUser, Long memberId) throws ApplicationException {
        // security start
        securityService.authzCanModifyProjectMember(currentUser, memberId);
        // security finish

        ProjectMember member = jdbcProjectMemberRepo.getNotFiredByIdAndFetchOrganizationMemberAndProject(memberId);
        if (member == null) {
            throw new NotFoundException("Project member");
        }
        delete(member);
    }

    @Override
    @Transactional(value = "jpaTransactionManager", rollbackFor = Throwable.class)
    public void leave(User currentUser, Long projectId) throws ApplicationException {
        // security start
        authnSecurityService.isAuthenticated(currentUser);
        // security finish

        ProjectMember member = jdbcProjectMemberRepo
                .getNotFiredByUserIdAndProjectIdAndFetchOrgMemberAndProject(
                        currentUser.getId(), projectId);
        if (member == null) {
            throw new NotFoundException("Project member");
        }
        delete(member);
    }

    @Override
    public List<OrganizationMemberUserDTO> search(
            User currentUser, String prefix, IdOrUiIdDTO projectDTO, boolean meFirst) throws Exception {
        // security start
        securityService.authzCanReadProjectMembers(currentUser, projectDTO);
        // security finish

        Long projectId = projectDTO.id;
        if (projectId == null) {
            projectId = jdbcProjectRepo.getProjectIdByUiId(projectDTO.uiId);
        }
        if (projectId == null) {
            return new ArrayList<OrganizationMemberUserDTO>();
        }

        SearchHits hits = elasticsearchService.searchByNameAndOrganizationAndProject(prefix, projectId);
        return OrganizationMemberUserDTO.fromSearchHits(hits, organizationId,
                ((meFirst && currentUser != null) ? currentUser.getId() : null));
    }
}
