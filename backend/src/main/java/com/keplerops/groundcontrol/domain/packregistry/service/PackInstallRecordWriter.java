package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.packregistry.model.PackInstallRecord;
import com.keplerops.groundcontrol.domain.packregistry.repository.PackInstallRecordRepository;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackInstallRecordWriter {

    private final PackInstallRecordRepository installRecordRepository;
    private final ProjectService projectService;

    public PackInstallRecordWriter(PackInstallRecordRepository installRecordRepository, ProjectService projectService) {
        this.installRecordRepository = installRecordRepository;
        this.projectService = projectService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PackInstallRecord save(PackInstallRecord record) {
        record.setProject(projectService.getById(record.getProject().getId()));
        return installRecordRepository.save(record);
    }
}
