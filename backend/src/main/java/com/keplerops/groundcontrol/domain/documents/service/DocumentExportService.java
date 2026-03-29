package com.keplerops.groundcontrol.domain.documents.service;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DocumentExportService {

    private final DocumentReadingOrderService readingOrderService;
    private final DocumentExportSdocService sdocService;
    private final DocumentExportHtmlService htmlService;
    private final DocumentExportPdfService pdfService;
    private final DocumentExportReqifService reqifService;
    private final RequirementRepository requirementRepository;
    private final RequirementRelationRepository relationRepository;

    public DocumentExportService(
            DocumentReadingOrderService readingOrderService,
            DocumentExportSdocService sdocService,
            DocumentExportHtmlService htmlService,
            DocumentExportPdfService pdfService,
            DocumentExportReqifService reqifService,
            RequirementRepository requirementRepository,
            RequirementRelationRepository relationRepository) {
        this.readingOrderService = readingOrderService;
        this.sdocService = sdocService;
        this.htmlService = htmlService;
        this.pdfService = pdfService;
        this.reqifService = reqifService;
        this.requirementRepository = requirementRepository;
        this.relationRepository = relationRepository;
    }

    public String exportToSdoc(UUID documentId) {
        var readingOrder = readingOrderService.getReadingOrder(documentId);
        Set<String> uids = collectRequirementUids(readingOrder.sections());
        Map<String, RequirementExportData> requirementsByUid = buildRequirementMap(uids);
        return sdocService.toSdoc(readingOrder, requirementsByUid);
    }

    public String exportToHtml(UUID documentId) {
        var readingOrder = readingOrderService.getReadingOrder(documentId);
        Set<String> uids = collectRequirementUids(readingOrder.sections());
        Map<String, RequirementExportData> requirementsByUid = buildRequirementMap(uids);
        return htmlService.toHtml(readingOrder, requirementsByUid);
    }

    public byte[] exportToPdf(UUID documentId) {
        var readingOrder = readingOrderService.getReadingOrder(documentId);
        Set<String> uids = collectRequirementUids(readingOrder.sections());
        Map<String, RequirementExportData> requirementsByUid = buildRequirementMap(uids);
        return pdfService.toPdf(readingOrder, requirementsByUid);
    }

    public String exportToReqif(UUID documentId) {
        var readingOrder = readingOrderService.getReadingOrder(documentId);
        Set<String> uids = collectRequirementUids(readingOrder.sections());
        Map<String, RequirementExportData> requirementsByUid = buildRequirementMap(uids);
        return reqifService.toReqif(readingOrder, requirementsByUid);
    }

    private Set<String> collectRequirementUids(List<ReadingOrderNode> sections) {
        Set<String> uids = new LinkedHashSet<>();
        for (var section : sections) {
            for (var item : section.content()) {
                if ("REQUIREMENT".equals(item.contentType()) && item.requirementUid() != null) {
                    uids.add(item.requirementUid());
                }
            }
            uids.addAll(collectRequirementUids(section.children()));
        }
        return uids;
    }

    private Map<String, RequirementExportData> buildRequirementMap(Set<String> uids) {
        Map<String, RequirementExportData> map = new HashMap<>();
        for (String uid : uids) {
            requirementRepository.findByUid(uid).ifPresent(req -> {
                List<String> parentUids = findParentUids(req.getId());
                map.put(
                        uid,
                        new RequirementExportData(
                                req.getUid(), req.getTitle(), req.getStatement(), buildComment(req), parentUids));
            });
        }
        return map;
    }

    private List<String> findParentUids(UUID requirementId) {
        List<RequirementRelation> relations = relationRepository.findBySourceId(requirementId);
        List<String> parentUids = new ArrayList<>();
        for (var rel : relations) {
            if (rel.getRelationType() == RelationType.PARENT) {
                parentUids.add(rel.getTarget().getUid());
            }
        }
        return parentUids;
    }

    private static String buildComment(Requirement req) {
        String rationale = req.getRationale();
        return rationale != null ? rationale : "";
    }
}
