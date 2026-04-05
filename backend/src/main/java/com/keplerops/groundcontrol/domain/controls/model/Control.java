package com.keplerops.groundcontrol.domain.controls.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.controls.state.ControlStatus;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.shared.persistence.JacksonTextCollectionConverters;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Map;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Audited
@Table(name = "control", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "uid"}))
public class Control extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 50)
    private String uid;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String objective;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_function", nullable = false, length = 20)
    private ControlFunction controlFunction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ControlStatus status = ControlStatus.DRAFT;

    @Column(length = 200)
    private String owner;

    @Column(name = "implementation_scope", columnDefinition = "TEXT")
    private String implementationScope;

    @Convert(converter = JacksonTextCollectionConverters.StringObjectMapConverter.class)
    @Column(name = "methodology_factors", columnDefinition = "TEXT")
    private Map<String, Object> methodologyFactors;

    @Convert(converter = JacksonTextCollectionConverters.StringObjectMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> effectiveness;

    @Column(length = 100)
    private String category;

    @Column(length = 200)
    private String source;

    protected Control() {
        // JPA
    }

    public Control(Project project, String uid, String title, ControlFunction controlFunction) {
        this.project = project;
        this.uid = uid;
        this.title = title;
        this.controlFunction = controlFunction;
    }

    public void transitionStatus(ControlStatus newStatus) {
        if (newStatus == null || !status.canTransitionTo(newStatus)) {
            throw new DomainValidationException(
                    "Cannot transition control status from " + status + " to " + newStatus,
                    "invalid_status_transition",
                    Map.of("current", status.name(), "requested", String.valueOf(newStatus)));
        }
        this.status = newStatus;
    }

    public Project getProject() {
        return project;
    }

    public String getUid() {
        return uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public ControlFunction getControlFunction() {
        return controlFunction;
    }

    public void setControlFunction(ControlFunction controlFunction) {
        this.controlFunction = controlFunction;
    }

    public ControlStatus getStatus() {
        return status;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getImplementationScope() {
        return implementationScope;
    }

    public void setImplementationScope(String implementationScope) {
        this.implementationScope = implementationScope;
    }

    public Map<String, Object> getMethodologyFactors() {
        return methodologyFactors;
    }

    public void setMethodologyFactors(Map<String, Object> methodologyFactors) {
        this.methodologyFactors = methodologyFactors;
    }

    public Map<String, Object> getEffectiveness() {
        return effectiveness;
    }

    public void setEffectiveness(Map<String, Object> effectiveness) {
        this.effectiveness = effectiveness;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
