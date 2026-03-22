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
    private static final String LOG_RELATION_FAILED = "import_relation_failed: source={} target={} error={}";

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
        var reqs = parsed.stream()
                .map(r -> new ParsedRequirement(r.uid(), r.title(), r.statement(), r.wave(), r.parentUids()))
                .toList();
        var counters = new ImportCounters();
        List<Map<String, Object>> errors = new ArrayList<>();

        var uidToId = upsertRequirements(projectId, reqs, counters, errors);
        createParentRelations(projectId, reqs, uidToId, counters, errors);
        createTraceabilityLinks(parsed, uidToId, counters, errors);

        return saveAuditAndBuildResult(ImportSourceType.STRICTDOC, filename, reqs.size(), counters, errors);
    }

    public ImportResult importReqif(UUID projectId, String filename, String content) {
        ReqifParseResult parsed = ReqifParser.parse(content);
        var reqs = parsed.requirements().stream()
                .map(r -> new ParsedRequirement(r.identifier(), r.title(), r.statement(), null, r.parentIdentifiers()))
                .toList();
        var counters = new ImportCounters();
        List<Map<String, Object>> errors = new ArrayList<>();

        var uidToId = upsertRequirements(projectId, reqs, counters, errors);
        createParentRelations(projectId, reqs, uidToId, counters, errors);
        createExplicitRelations(projectId, parsed.relations(), uidToId, counters, errors);

        return saveAuditAndBuildResult(ImportSourceType.REQIF, filename, reqs.size(), counters, errors);
    }

    private Map<String, UUID> upsertRequirements(
            UUID projectId,
            List<ParsedRequirement> requirements,
            ImportCounters counters,
            List<Map<String, Object>> errors) {
        Map<String, UUID> uidToId = new HashMap<>();
        for (ParsedRequirement req : requirements) {
            try {
                var existing = requirementRepository.findByProjectIdAndUidIgnoreCase(projectId, req.uid());
                UUID reqId;
                if (existing.isPresent()) {
                    var cmd = new UpdateRequirementCommand(
                            req.title(), req.statement(), null, RequirementType.FUNCTIONAL, Priority.MUST, req.wave());
                    var updated = requirementService.update(existing.get().getId(), cmd);
                    reqId = updated.getId();
                    counters.requirementsUpdated++;
                } else {
                    var cmd = new CreateRequirementCommand(
                            projectId,
                            req.uid(),
                            req.title(),
                            req.statement(),
                            null,
                            RequirementType.FUNCTIONAL,
                            Priority.MUST,
                            req.wave());
                    var created = requirementService.create(cmd);
                    reqId = created.getId();
                    counters.requirementsCreated++;
                }
                uidToId.put(req.uid(), reqId);
            } catch (ConflictException | NotFoundException | DomainValidationException e) {
                log.warn("import_requirement_failed: uid={} error={}", req.uid(), e.getMessage());
                errors.add(Map.of("phase", "requirements", "uid", req.uid(), "error", e.getMessage()));
            }
        }
        return uidToId;
    }

    private void createParentRelations(
            UUID projectId,
            List<ParsedRequirement> requirements,
            Map<String, UUID> uidToId,
            ImportCounters counters,
            List<Map<String, Object>> errors) {
        for (ParsedRequirement req : requirements) {
            UUID childId = uidToId.get(req.uid());
            if (childId == null) {
                continue;
            }
            for (String parentUid : req.parentUids()) {
                try {
                    UUID parentId = resolveRequirementId(projectId, parentUid, uidToId);
                    if (parentId == null) {
                        errors.add(Map.of(
                                "phase", "relations", "uid", req.uid(), "error", "Parent not found: " + parentUid));
                        continue;
                    }
                    if (relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                            childId, parentId, RelationType.PARENT)) {
                        counters.relationsSkipped++;
                        continue;
                    }
                    requirementService.createRelation(childId, parentId, RelationType.PARENT);
                    counters.relationsCreated++;
                } catch (ConflictException | NotFoundException | DomainValidationException e) {
                    log.warn(LOG_RELATION_FAILED, req.uid(), parentUid, e.getMessage());
                    errors.add(Map.of(
                            "phase", "relations", "uid", req.uid(), "parent", parentUid, "error", e.getMessage()));
                }
            }
        }
    }

    private UUID resolveRequirementId(UUID projectId, String uid, Map<String, UUID> uidToId) {
        UUID id = uidToId.get(uid);
        if (id != null) {
            return id;
        }
        var opt = requirementRepository.findByProjectIdAndUidIgnoreCase(projectId, uid);
        return opt.map(r -> r.getId()).orElse(null);
    }

    private void createExplicitRelations(
            UUID projectId,
            List<ReqifRelation> relations,
            Map<String, UUID> uidToId,
            ImportCounters counters,
            List<Map<String, Object>> errors) {
        for (ReqifRelation rel : relations) {
            try {
                UUID sourceId = resolveRequirementId(projectId, rel.sourceIdentifier(), uidToId);
                if (sourceId == null) {
                    errors.add(Map.of(
                            "phase",
                            "relations",
                            "uid",
                            rel.sourceIdentifier(),
                            "error",
                            "Source not found: " + rel.sourceIdentifier()));
                    continue;
                }
                UUID targetId = resolveRequirementId(projectId, rel.targetIdentifier(), uidToId);
                if (targetId == null) {
                    errors.add(Map.of(
                            "phase",
                            "relations",
                            "uid",
                            rel.targetIdentifier(),
                            "error",
                            "Target not found: " + rel.targetIdentifier()));
                    continue;
                }
                if (relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                        sourceId, targetId, rel.relationType())) {
                    counters.relationsSkipped++;
                    continue;
                }
                requirementService.createRelation(sourceId, targetId, rel.relationType());
                counters.relationsCreated++;
            } catch (ConflictException | NotFoundException | DomainValidationException e) {
                log.warn(LOG_RELATION_FAILED, rel.sourceIdentifier(), rel.targetIdentifier(), e.getMessage());
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
    }

    private void createTraceabilityLinks(
            List<SdocRequirement> parsed,
            Map<String, UUID> uidToId,
            ImportCounters counters,
            List<Map<String, Object>> errors) {
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
                        counters.traceabilityLinksSkipped++;
                        continue;
                    }
                    var cmd = new CreateTraceabilityLinkCommand(
                            ArtifactType.GITHUB_ISSUE, artifactId, null, null, LinkType.IMPLEMENTS);
                    traceabilityService.createLink(reqId, cmd);
                    counters.traceabilityLinksCreated++;
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
    }

    private ImportResult saveAuditAndBuildResult(
            ImportSourceType sourceType,
            String filename,
            int parsedCount,
            ImportCounters counters,
            List<Map<String, Object>> errors) {
        var audit = new RequirementImport(sourceType);
        audit.setSourceFile(filename);
        audit.setStats(Map.of(
                "requirementsParsed", parsedCount,
                "requirementsCreated", counters.requirementsCreated,
                "requirementsUpdated", counters.requirementsUpdated,
                "relationsCreated", counters.relationsCreated,
                "relationsSkipped", counters.relationsSkipped,
                "traceabilityLinksCreated", counters.traceabilityLinksCreated,
                "traceabilityLinksSkipped", counters.traceabilityLinksSkipped));
        audit.setErrors(errors);
        var savedAudit = importRepository.save(audit);

        return new ImportResult(
                savedAudit.getId(),
                savedAudit.getImportedAt(),
                parsedCount,
                counters.requirementsCreated,
                counters.requirementsUpdated,
                counters.relationsCreated,
                counters.relationsSkipped,
                counters.traceabilityLinksCreated,
                counters.traceabilityLinksSkipped,
                errors);
    }
}
