package org.wrkr.clb.model.project.task;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wrkr.clb.model.BaseIdEntity;
import org.wrkr.clb.model.BaseVersionedEntityMeta;
import org.wrkr.clb.model.VersionedIdEntity;
import org.wrkr.clb.model.project.Project;
import org.wrkr.clb.model.project.ProjectMember;
import org.wrkr.clb.model.project.roadmap.TaskCard;
import org.wrkr.clb.model.project.task.attachment.Attachment;

@Entity
@Table(name = TaskMeta.TABLE_NAME)
public class Task extends BaseIdEntity implements VersionedIdEntity {

    public static final int SUMMARY_LENGTH = 80;

    @Column(name = BaseVersionedEntityMeta.recordVersion, nullable = false)
    private Long recordVersion = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @Transient
    private Long projectId;

    @Column(name = "number", nullable = false)
    private Long number;

    @Column(name = "description_md", nullable = false)
    private String descriptionMd;

    @Column(name = "description_html", nullable = false)
    private String descriptionHtml;

    @Column(name = "summary", nullable = false, length = SUMMARY_LENGTH)
    private String summary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = TaskMeta.reporterId, nullable = false)
    private ProjectMember reporter;
    @Transient
    private Long reporterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = TaskMeta.assigneeId, nullable = true)
    private ProjectMember assignee;
    @Transient
    private Long assigneeId;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private TaskType type;
    @Transient
    private Long typeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "priority_id", nullable = false)
    private TaskPriority priority;
    @Transient
    private Long priorityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private TaskStatus status;
    @Transient
    private Long statusId;

    @Column(name = "hidden", nullable = false)
    private boolean hidden = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_card_id", nullable = true)
    private TaskCard card;
    @Transient
    private Long cardId;

    @Transient
    private Long jiraTaskId;

    @Transient
    private List<Task> linkedTasks = new ArrayList<Task>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = TaskHashtagToTaskMeta.TABLE_NAME, joinColumns = {
            @JoinColumn(name = TaskHashtagToTaskMeta.taskId) }, inverseJoinColumns = {
                    @JoinColumn(name = TaskHashtagToTaskMeta.taskHashtagId) })
    private List<TaskHashtag> hashtags = new ArrayList<TaskHashtag>();

    @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<Attachment>();

    @Override
    public Long getRecordVersion() {
        return recordVersion;
    }

    @Override
    public void setRecordVersion(Long recordVersion) {
        this.recordVersion = recordVersion;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescriptionMd() {
        return descriptionMd;
    }

    public void setDescriptionMd(String descriptionMd) {
        this.descriptionMd = descriptionMd;
    }

    public String getDescriptionHtml() {
        return descriptionHtml;
    }

    public void setDescriptionHtml(String descriptionHtml) {
        this.descriptionHtml = descriptionHtml;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
        this.projectId = project.getId();
    }

    public ProjectMember getReporter() {
        return reporter;
    }

    public void setReporter(ProjectMember reporter) {
        this.reporter = reporter;
        this.reporterId = reporter.getId();
    }

    public ProjectMember getAssignee() {
        return assignee;
    }

    public void setAssignee(ProjectMember assignee) {
        this.assignee = assignee;
        this.assigneeId = (assignee == null ? null : assignee.getId());
    }

    public Long getProjectId() {
        if (projectId == null) {
            return (project == null ? null : project.getId());
        }
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
        this.project = null;
    }

    public Long getReporterId() {
        if (reporterId == null) {
            return (reporter == null ? null : reporter.getId());
        }
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
        this.reporter = null;
    }

    public Long getAssigneeId() {
        if (assigneeId == null) {
            return (assignee == null ? null : assignee.getId());
        }
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
        this.assignee = null;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
        this.typeId = type.getId();
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
        this.priorityId = priority.getId();
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
        this.statusId = status.getId();
    }

    public Long getTypeId() {
        if (typeId == null) {
            return (type == null ? null : type.getId());
        }
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
        this.type = null;
    }

    public Long getPriorityId() {
        if (priorityId == null) {
            return (priority == null ? null : priority.getId());
        }
        return priorityId;
    }

    public void setPriorityId(Long priorityId) {
        this.priorityId = priorityId;
        this.priority = null;
    }

    public Long getStatusId() {
        if (statusId == null) {
            return (status == null ? null : status.getId());
        }
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
        this.status = null;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public TaskCard getCard() {
        return card;
    }

    public void setCard(TaskCard card) {
        this.card = card;
        this.cardId = (card == null ? null : card.getId());
    }

    public Long getCardId() {
        if (cardId == null) {
            return (card == null ? null : card.getId());
        }
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
        this.card = null;
    }

    public Long getJiraTaskId() {
        return jiraTaskId;
    }

    public void setJiraTaskId(Long jiraTaskId) {
        this.jiraTaskId = jiraTaskId;
    }

    public List<Task> getLinkedTasks() {
        return linkedTasks;
    }

    public void setLinkedTasks(List<Task> linkedTasks) {
        this.linkedTasks = linkedTasks;
    }

    public List<TaskHashtag> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<TaskHashtag> hashtags) {
        this.hashtags = hashtags;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }
}
