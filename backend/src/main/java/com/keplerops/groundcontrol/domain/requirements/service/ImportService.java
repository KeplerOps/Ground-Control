package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementImport;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementImportRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.ImportSourceType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.Priority;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import com.keplerops.groundcontrol.domain.requirements.state.RequirementType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final RequirementService requirementService;
    private final TraceabilityService traceabilityService;
    private final RequirementRepository requirementRepository;
    private final RequirementRelationRepository relationRepository;
    private final TraceabilityLinkRepository traceabilityLinkRepository;
    private final RequirementImportRepository importRepository;

    public ImportService(
            RequirementService requirementService,
            TraceabilityService traceabilityService,
            RequirementRepository requirementRepository,
            RequirementRelationRepository relationRepository,
            TraceabilityLinkRepository traceabilityLinkRepository,
            RequirementImportRepository importRepository) {
        this.requirementService = requirementService;
        this.traceabilityService = traceabilityService;
        this.requirementRepository = requirementRepository;
        this.relationRepository = relationRepository;
        this.traceabilityLinkRepository = traceabilityLinkRepository;
        this.importRepository = importRepository;
    }

    public ImportResult importStrictdoc(UUID projectId, String filename, String content) {
        List<SdocRequirement> parsed = SdocParser.parse(content);
        List<Map<String, Object>> errors = new ArrayList<>();

        int requirementsCreated = 0;
        int requirementsUpdated = 0;
        int relationsCreated = 0;
        int relationsSkipped = 0;
        int traceabilityLinksCreated = 0;
        int traceabilityLinksSkipped = 0;

        // Phase 1: Upsert requirements
        Map<String, UUID> uidToId = new HashMap<>();
        for (SdocRequirement sdocReq : parsed) {
            try {
                var existing = requirementRepository.findByProjectIdAndUid(projectId, sdocReq.uid());
                UUID reqId;
                if (existing.isPresent()) {
                    var cmd = new UpdateRequirementCommand(
                            sdocReq.title(),
                            sdocReq.statement(),
                            null,
                            RequirementType.FUNCTIONAL,
                            Priority.MUST,
                            sdocReq.wave());
                    var updated = requirementService.update(existing.get().getId(), cmd);
                    reqId = updated.getId();
                    requirementsUpdated++;
                } else {
                    var cmd = new CreateRequirementCommand(
                            projectId,
                            sdocReq.uid(),
                            sdocReq.title(),
                            sdocReq.statement(),
                            null,
                            RequirementType.FUNCTIONAL,
                            Priority.MUST,
                            sdocReq.wave());
                    var created = requirementService.create(cmd);
                    reqId = created.getId();
                    requirementsCreated++;
                }
                uidToId.put(sdocReq.uid(), reqId);
            } catch (ConflictException | NotFoundException | DomainValidationException e) {
                log.warn("import_requirement_failed: uid={} error={}", sdocReq.uid(), e.getMessage());
                errors.add(Map.of("phase", "requirements", "uid", sdocReq.uid(), "error", e.getMessage()));
            }
        }

        // Phase 2: Create relations
        for (SdocRequirement sdocReq : parsed) {
            UUID childId = uidToId.get(sdocReq.uid());
            if (childId == null) {
                continue;
            }
            for (String parentUid : sdocReq.parentUids()) {
                try {
                    UUID parentId = uidToId.get(parentUid);
                    if (parentId == null) {
                        var parentOpt = requirementRepository.findByProjectIdAndUid(projectId, parentUid);
                        if (parentOpt.isEmpty()) {
                            errors.add(Map.of(
                                    "phase",
                                    "relations",
                                    "uid",
                                    sdocReq.uid(),
                                    "error",
                                    "Parent not found: " + parentUid));
                            continue;
                        }
                        parentId = parentOpt.get().getId();
                    }
                    if (relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                            childId, parentId, RelationType.PARENT)) {
                        relationsSkipped++;
                        continue;
                    }
                    requirementService.createRelation(childId, parentId, RelationType.PARENT);
                    relationsCreated++;
                } catch (ConflictException | NotFoundException | DomainValidationException e) {
                    log.warn(
                            "import_relation_failed: source={} target={} error={}",
                            sdocReq.uid(),
                            parentUid,
                            e.getMessage());
                    errors.add(Map.of(
                            "phase", "relations", "uid", sdocReq.uid(), "parent", parentUid, "error", e.getMessage()));
                }
            }
        }

        // Phase 3: Create traceability links
        for (SdocRequirement sdocReq : parsed) {
            UUID reqId = uidToId.get(sdocReq.uid());
            if (reqId == null) {
                continue;
            }
            for (Integer issueNum : sdocReq.issueRefs()) {
                try {
                    String artifactId = String.valueOf(issueNum);
                    if (traceabilityLinkRepository.existsByRequirementIdAndArtifactTypeAndArtifactIdentifierAndLinkType(
                            reqId, ArtifactType.GITHUB_ISSUE, artifactId, LinkType.IMPLEMENTS)) {
                        traceabilityLinksSkipped++;
                        continue;
                    }
                    var cmd = new CreateTraceabilityLinkCommand(
                            ArtifactType.GITHUB_ISSUE, artifactId, null, null, LinkType.IMPLEMENTS);
                    traceabilityService.createLink(reqId, cmd);
                    traceabilityLinksCreated++;
                } catch (ConflictException | NotFoundException | DomainValidationException e) {
                    log.warn(
                            "import_traceability_link_failed: uid={} issue={} error={}",
                            sdocReq.uid(),
                            issueNum,
                            e.getMessage());
                    errors.add(Map.of(
                            "phase",
                            "traceability",
                            "uid",
                            sdocReq.uid(),
                            "issueRef",
                            String.valueOf(issueNum),
                            "error",
                            e.getMessage()));
                }
            }
        }

        // Save audit record
        var audit = new RequirementImport(ImportSourceType.STRICTDOC);
        audit.setSourceFile(filename);
        audit.setStats(Map.of(
                "requirementsParsed", parsed.size(),
                "requirementsCreated", requirementsCreated,
                "requirementsUpdated", requirementsUpdated,
                "relationsCreated", relationsCreated,
                "relationsSkipped", relationsSkipped,
                "traceabilityLinksCreated", traceabilityLinksCreated,
                "traceabilityLinksSkipped", traceabilityLinksSkipped));
        audit.setErrors(errors);
        var savedAudit = importRepository.save(audit);

        return new ImportResult(
                savedAudit.getId(),
                savedAudit.getImportedAt(),
                parsed.size(),
                requirementsCreated,
                requirementsUpdated,
                relationsCreated,
                relationsSkipped,
                traceabilityLinksCreated,
                traceabilityLinksSkipped,
                errors);
    }

    public ImportResult importReqif(UUID projectId, String filename, String content) {
        ReqifParseResult parsed = ReqifParser.parse(content);
        List<Map<String, Object>> errors = new ArrayList<>();

        int requirementsCreated = 0;
        int requirementsUpdated = 0;
        int relationsCreated = 0;
        int relationsSkipped = 0;

        // Phase 1: Upsert requirements
        Map<String, UUID> uidToId = new HashMap<>();
        for (ReqifRequirement reqifReq : parsed.requirements()) {
            try {
                var existing = requirementRepository.findByProjectIdAndUid(projectId, reqifReq.identifier());
                UUID reqId;
                if (existing.isPresent()) {
                    var cmd = new UpdateRequirementCommand(
                            reqifReq.title(),
                            reqifReq.statement(),
                            null,
                            RequirementType.FUNCTIONAL,
                            Priority.MUST,
                            null);
                    var updated = requirementService.update(existing.get().getId(), cmd);
                    reqId = updated.getId();
                    requirementsUpdated++;
                } else {
                    var cmd = new CreateRequirementCommand(
                            projectId,
                            reqifReq.identifier(),
                            reqifReq.title(),
                            reqifReq.statement(),
                            null,
                            RequirementType.FUNCTIONAL,
                            Priority.MUST,
                            null);
                    var created = requirementService.create(cmd);
                    reqId = created.getId();
                    requirementsCreated++;
                }
                uidToId.put(reqifReq.identifier(), reqId);
            } catch (ConflictException | NotFoundException | DomainValidationException e) {
                log.warn("import_requirement_failed: uid={} error={}", reqifReq.identifier(), e.getMessage());
                errors.add(Map.of("phase", "requirements", "uid", reqifReq.identifier(), "error", e.getMessage()));
            }
        }

        // Phase 2: Create relations from hierarchy parents
        for (ReqifRequirement reqifReq : parsed.requirements()) {
            UUID childId = uidToId.get(reqifReq.identifier());
            if (childId == null) {
                continue;
            }
            for (String parentUid : reqifReq.parentIdentifiers()) {
                try {
                    UUID parentId = uidToId.get(parentUid);
                    if (parentId == null) {
                        var parentOpt = requirementRepository.findByProjectIdAndUid(projectId, parentUid);
                        if (parentOpt.isEmpty()) {
                            errors.add(Map.of(
                                    "phase",
                                    "relations",
                                    "uid",
                                    reqifReq.identifier(),
                                    "error",
                                    "Parent not found: " + parentUid));
                            continue;
                        }
                        parentId = parentOpt.get().getId();
                    }
                    if (relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                            childId, parentId, RelationType.PARENT)) {
                        relationsSkipped++;
                        continue;
                    }
                    requirementService.createRelation(childId, parentId, RelationType.PARENT);
                    relationsCreated++;
                } catch (ConflictException | NotFoundException | DomainValidationException e) {
                    log.warn(
                            "import_relation_failed: source={} target={} error={}",
                            reqifReq.identifier(),
                            parentUid,
                            e.getMessage());
                    errors.add(Map.of(
                            "phase",
                            "relations",
                            "uid",
                            reqifReq.identifier(),
                            "parent",
                            parentUid,
                            "error",
                            e.getMessage()));
                }
            }
        }

        // Phase 2b: Create relations from explicit SpecRelations.
        // If a relation was already created from hierarchy nesting (Phase 2), the existsBy... check
        // will skip it here — the skipped count may include these overlapping relations.
        for (ReqifRelation rel : parsed.relations()) {
            try {
                UUID sourceId = uidToId.get(rel.sourceIdentifier());
                UUID targetId = uidToId.get(rel.targetIdentifier());
                if (sourceId == null) {
                    var opt = requirementRepository.findByProjectIdAndUid(projectId, rel.sourceIdentifier());
                    if (opt.isEmpty()) {
                        errors.add(Map.of(
                                "phase",
                                "relations",
                                "uid",
                                rel.sourceIdentifier(),
                                "error",
                                "Source not found: " + rel.sourceIdentifier()));
                        continue;
                    }
                    sourceId = opt.get().getId();
                }
                if (targetId == null) {
                    var opt = requirementRepository.findByProjectIdAndUid(projectId, rel.targetIdentifier());
                    if (opt.isEmpty()) {
                        errors.add(Map.of(
                                "phase",
                                "relations",
                                "uid",
                                rel.targetIdentifier(),
                                "error",
                                "Target not found: " + rel.targetIdentifier()));
                        continue;
                    }
                    targetId = opt.get().getId();
                }
                if (relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                        sourceId, targetId, rel.relationType())) {
                    relationsSkipped++;
                    continue;
                }
                requirementService.createRelation(sourceId, targetId, rel.relationType());
                relationsCreated++;
            } catch (ConflictException | NotFoundException | DomainValidationException e) {
                log.warn(
                        "import_relation_failed: source={} target={} error={}",
                        rel.sourceIdentifier(),
                        rel.targetIdentifier(),
                        e.getMessage());
                errors.add(Map.of(
                        "phase",
                        "relations",
                        "uid",
                        rel.sourceIdentifier(),
                        "target",
                        rel.targetIdentifier(),
                        "error",
                        e.getMessage()));
            }
        }

        // Phase 3: No-op — ReqIF has no GitHub issue references
        int traceabilityLinksCreated = 0;
        int traceabilityLinksSkipped = 0;

        // Save audit record
        var audit = new RequirementImport(ImportSourceType.REQIF);
        audit.setSourceFile(filename);
        audit.setStats(Map.of(
                "requirementsParsed", parsed.requirements().size(),
                "requirementsCreated", requirementsCreated,
                "requirementsUpdated", requirementsUpdated,
                "relationsCreated", relationsCreated,
                "relationsSkipped", relationsSkipped,
                "traceabilityLinksCreated", traceabilityLinksCreated,
                "traceabilityLinksSkipped", traceabilityLinksSkipped));
        audit.setErrors(errors);
        var savedAudit = importRepository.save(audit);

        return new ImportResult(
                savedAudit.getId(),
                savedAudit.getImportedAt(),
                parsed.requirements().size(),
                requirementsCreated,
                requirementsUpdated,
                relationsCreated,
                relationsSkipped,
                traceabilityLinksCreated,
                traceabilityLinksSkipped,
                errors);
    }
}
