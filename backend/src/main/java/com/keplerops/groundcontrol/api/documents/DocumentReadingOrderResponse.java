package com.keplerops.groundcontrol.api.documents;

import com.keplerops.groundcontrol.domain.documents.service.DocumentReadingOrder;
import com.keplerops.groundcontrol.domain.documents.service.ReadingOrderContentItem;
import com.keplerops.groundcontrol.domain.documents.service.ReadingOrderNode;
import java.util.List;
import java.util.UUID;

public record DocumentReadingOrderResponse(
        UUID documentId, String title, String version, String description, List<SectionNode> sections) {

    public record ContentItem(
            String contentType, String requirementUid, String requirementTitle, String textContent, int sortOrder) {

        public static ContentItem from(ReadingOrderContentItem item) {
            return new ContentItem(
                    item.contentType(),
                    item.requirementUid(),
                    item.requirementTitle(),
                    item.textContent(),
                    item.sortOrder());
        }
    }

    public record SectionNode(
            UUID id,
            String title,
            String description,
            int sortOrder,
            List<ContentItem> content,
            List<SectionNode> children) {

        public static SectionNode from(ReadingOrderNode node) {
            return new SectionNode(
                    node.id(),
                    node.title(),
                    node.description(),
                    node.sortOrder(),
                    node.content().stream().map(ContentItem::from).toList(),
                    node.children().stream().map(SectionNode::from).toList());
        }
    }

    public static DocumentReadingOrderResponse from(DocumentReadingOrder order) {
        return new DocumentReadingOrderResponse(
                order.documentId(),
                order.title(),
                order.version(),
                order.description(),
                order.sections().stream().map(SectionNode::from).toList());
    }
}
