package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Pure-Java parser for ReqIF 1.2 XML files. Uses DOM/JAXP (built into Java 21). Secure by default:
 * DTDs and external entities are disabled (XXE prevention).
 */
public final class ReqifParser {

    private static final int MAX_UID_LENGTH = 50;

    private ReqifParser() {}

    public static ReqifParseResult parse(String xml) {
        Document doc = parseSecureXml(xml);
        Element root = doc.getDocumentElement();

        // Build specType attribute definitions: specTypeId -> (attrDefId -> attrName)
        Map<String, Map<String, String>> specTypeAttrDefs = parseSpecTypes(root);

        // Parse SPEC-OBJECTS
        Map<String, ReqifRequirement> requirementsByIdentifier = parseSpecObjects(root, specTypeAttrDefs);

        // Parse SPEC-RELATIONS (explicit relations)
        List<ReqifRelation> explicitRelations = parseSpecRelations(root);

        // Parse SPECIFICATIONS hierarchy (parent-child from nesting)
        applySpecificationHierarchy(root, requirementsByIdentifier);

        return new ReqifParseResult(List.copyOf(requirementsByIdentifier.values()), explicitRelations);
    }

    private static Document parseSecureXml(String xml) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            // XXE prevention
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);

            var builder = factory.newDocumentBuilder();
            var is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            return builder.parse(is);
        } catch (Exception e) {
            throw new ReqifParseException("Failed to parse ReqIF XML: " + e.getMessage(), e);
        }
    }

    /**
     * Parse SPEC-TYPES to build a map of specTypeId -> (attrDefId -> attrName). This is needed
     * because SPEC-OBJECT attribute values reference attrDef IDs, not names.
     */
    private static Map<String, Map<String, String>> parseSpecTypes(Element root) {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (Element specType : findElements(root, "SPEC-OBJECT-TYPE")) {
            String typeId = specType.getAttribute("IDENTIFIER");
            Map<String, String> attrDefs = new HashMap<>();
            // Collect from all ATTRIBUTE-DEFINITION-* elements
            collectAttrDefs(specType, attrDefs);
            result.put(typeId, attrDefs);
        }
        // Also parse SPEC-RELATION-TYPE for relation type names
        for (Element relType : findElements(root, "SPEC-RELATION-TYPE")) {
            String typeId = relType.getAttribute("IDENTIFIER");
            Map<String, String> attrDefs = new HashMap<>();
            collectAttrDefs(relType, attrDefs);
            result.put(typeId, attrDefs);
        }
        // Also parse SPECIFICATION-TYPE
        for (Element specType : findElements(root, "SPECIFICATION-TYPE")) {
            String typeId = specType.getAttribute("IDENTIFIER");
            Map<String, String> attrDefs = new HashMap<>();
            collectAttrDefs(specType, attrDefs);
            result.put(typeId, attrDefs);
        }
        return result;
    }

    private static void collectAttrDefs(Element parent, Map<String, String> attrDefs) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element el) {
                if (el.getTagName().startsWith("ATTRIBUTE-DEFINITION-")) {
                    attrDefs.put(el.getAttribute("IDENTIFIER"), el.getAttribute("LONG-NAME"));
                }
                // Recurse into containers like SPEC-ATTRIBUTES
                collectAttrDefs(el, attrDefs);
            }
        }
    }

    private static Map<String, ReqifRequirement> parseSpecObjects(
            Element root, Map<String, Map<String, String>> specTypeAttrDefs) {
        Map<String, ReqifRequirement> result = new LinkedHashMap<>();
        for (Element specObj : findElements(root, "SPEC-OBJECT")) {
            String identifier = specObj.getAttribute("IDENTIFIER");
            if (identifier == null || identifier.isBlank()) {
                continue;
            }

            String longName = specObj.getAttribute("LONG-NAME");

            // Resolve the spec type for this object
            String specTypeId = resolveTypeRef(specObj, "SPEC-OBJECT-TYPE-REF");
            Map<String, String> attrDefMap =
                    specTypeId != null ? specTypeAttrDefs.getOrDefault(specTypeId, Map.of()) : Map.of();

            // Extract attribute values
            Map<String, String> attrValues = extractAttributeValues(specObj, attrDefMap);

            String title = resolveTitle(longName, attrValues);
            String statement = resolveStatement(attrValues);
            String uid = sanitizeUid(identifier);

            result.put(identifier, new ReqifRequirement(uid, title, statement, List.of(), List.of()));
        }
        return result;
    }

    private static List<ReqifRelation> parseSpecRelations(Element root) {
        List<ReqifRelation> relations = new ArrayList<>();
        for (Element specRel : findElements(root, "SPEC-RELATION")) {
            String sourceId = getRefText(specRel, "SOURCE-REF");
            String targetId = getRefText(specRel, "TARGET-REF");
            if (sourceId == null || targetId == null) {
                continue;
            }

            // Determine relation type from SPEC-RELATION-TYPE long name
            String relTypeId = resolveTypeRef(specRel, "SPEC-RELATION-TYPE-REF");
            String typeName = "";
            if (relTypeId != null) {
                // Try to get LONG-NAME from the type element itself (stored in specTypeAttrDefs key)
                typeName = resolveRelationTypeName(root, relTypeId);
            }
            if (typeName.isBlank()) {
                typeName = specRel.getAttribute("LONG-NAME");
            }

            RelationType relationType = mapRelationType(typeName);

            relations.add(new ReqifRelation(sanitizeUid(sourceId), sanitizeUid(targetId), relationType));
        }
        return relations;
    }

    private static String resolveRelationTypeName(Element root, String typeId) {
        for (Element relType : findElements(root, "SPEC-RELATION-TYPE")) {
            if (typeId.equals(relType.getAttribute("IDENTIFIER"))) {
                return relType.getAttribute("LONG-NAME");
            }
        }
        return "";
    }

    private static void applySpecificationHierarchy(Element root, Map<String, ReqifRequirement> requirements) {
        for (Element specification : findElements(root, "SPECIFICATION")) {
            // Walk CHILDREN recursively
            for (Element children : directChildElements(specification, "CHILDREN")) {
                walkHierarchy(children, null, requirements);
            }
        }
    }

    private static void walkHierarchy(
            Element childrenElement, String parentIdentifier, Map<String, ReqifRequirement> requirements) {
        for (Element specHierarchy : directChildElements(childrenElement, "SPEC-HIERARCHY")) {
            String objRef = getRefText(specHierarchy, "OBJECT-REF");
            if (objRef == null) {
                continue;
            }

            // If this object has a parent in the hierarchy, record the parent relationship
            if (parentIdentifier != null) {
                ReqifRequirement existing = requirements.get(objRef);
                if (existing != null) {
                    List<String> newParents = new ArrayList<>(existing.parentIdentifiers());
                    String parentUid = sanitizeUid(parentIdentifier);
                    if (!newParents.contains(parentUid)) {
                        newParents.add(parentUid);
                    }
                    requirements.put(
                            objRef,
                            new ReqifRequirement(
                                    existing.identifier(),
                                    existing.title(),
                                    existing.statement(),
                                    newParents,
                                    existing.relations()));
                }
            }

            // Recurse into nested CHILDREN
            for (Element nestedChildren : directChildElements(specHierarchy, "CHILDREN")) {
                walkHierarchy(nestedChildren, objRef, requirements);
            }
        }
    }

    // ---- Attribute resolution helpers ----

    private static Map<String, String> extractAttributeValues(Element specObj, Map<String, String> attrDefMap) {
        Map<String, String> values = new HashMap<>();
        NodeList children = specObj.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element container) {
                // Look for VALUES or ATTRIBUTE-VALUE-* elements
                extractAttrValuesRecursive(container, attrDefMap, values);
            }
        }
        return values;
    }

    private static void extractAttrValuesRecursive(
            Element el, Map<String, String> attrDefMap, Map<String, String> values) {
        String tag = el.getTagName();
        if (tag.startsWith("ATTRIBUTE-VALUE-")) {
            String defRef = getAttrDefRef(el);
            String attrName = defRef != null ? attrDefMap.getOrDefault(defRef, defRef) : null;
            String value = extractAttrValueText(el);
            if (attrName != null && value != null) {
                values.put(attrName, value);
            }
            return;
        }
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child) {
                extractAttrValuesRecursive(child, attrDefMap, values);
            }
        }
    }

    private static String getAttrDefRef(Element attrValue) {
        // Look for DEFINITION child containing an ATTRIBUTE-DEFINITION-*-REF
        for (Element def : directChildElements(attrValue, "DEFINITION")) {
            NodeList refs = def.getChildNodes();
            for (int i = 0; i < refs.getLength(); i++) {
                if (refs.item(i) instanceof Element refEl) {
                    String text = refEl.getTextContent();
                    if (text != null && !text.isBlank()) {
                        return text.trim();
                    }
                }
            }
        }
        return null;
    }

    private static String extractAttrValueText(Element attrValue) {
        // For ATTRIBUTE-VALUE-STRING / ATTRIBUTE-VALUE-INTEGER, use THE-VALUE attribute
        String theValue = attrValue.getAttribute("THE-VALUE");
        if (theValue != null && !theValue.isBlank()) {
            return theValue;
        }
        // For ATTRIBUTE-VALUE-XHTML, look for THE-VALUE child containing XHTML
        for (Element theValueEl : directChildElements(attrValue, "THE-VALUE")) {
            return stripXhtml(theValueEl);
        }
        return null;
    }

    static String stripXhtml(Element element) {
        return element.getTextContent().replaceAll("\\s+", " ").trim();
    }

    // ---- Title/statement resolution with fallback ----

    private static String resolveTitle(String longName, Map<String, String> attrValues) {
        // Priority: LONG-NAME attribute, then attribute values by common names
        if (longName != null && !longName.isBlank()) {
            return longName;
        }
        for (String key : List.of("ReqIF.Name", "Name", "Title")) {
            String val = attrValues.get(key);
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return "";
    }

    private static String resolveStatement(Map<String, String> attrValues) {
        for (String key : List.of("ReqIF.Text", "Text", "Description")) {
            String val = attrValues.get(key);
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return "";
    }

    // ---- Relation type mapping ----

    public static RelationType mapRelationType(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return RelationType.RELATED;
        }
        String lower = typeName.toLowerCase(Locale.ROOT);
        if (lower.contains("parent") || lower.contains("child") || lower.contains("hierarchy")) {
            return RelationType.PARENT;
        }
        if (lower.contains("depend")) {
            return RelationType.DEPENDS_ON;
        }
        if (lower.contains("conflict")) {
            return RelationType.CONFLICTS_WITH;
        }
        if (lower.contains("refine")) {
            return RelationType.REFINES;
        }
        if (lower.contains("supersede") || lower.contains("replace")) {
            return RelationType.SUPERSEDES;
        }
        return RelationType.RELATED;
    }

    // ---- UID sanitization ----

    public static String sanitizeUid(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "";
        }
        if (identifier.length() <= MAX_UID_LENGTH) {
            return identifier;
        }
        // Deterministic truncation: first 42 chars + "-" + 7 char SHA-256 hash
        String hash = sha256Prefix(identifier, 7);
        return identifier.substring(0, 42) + "-" + hash;
    }

    private static String sha256Prefix(String input, int length) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, length);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ---- DOM utility methods ----

    private static String resolveTypeRef(Element parent, String refTagName) {
        // Look in TYPE child element for the ref
        for (Element typeContainer : directChildElements(parent, "TYPE")) {
            NodeList refs = typeContainer.getChildNodes();
            for (int i = 0; i < refs.getLength(); i++) {
                if (refs.item(i) instanceof Element refEl && refEl.getTagName().equals(refTagName)) {
                    String text = refEl.getTextContent();
                    if (text != null && !text.isBlank()) {
                        return text.trim();
                    }
                }
            }
        }
        return null;
    }

    private static String getRefText(Element parent, String refTagName) {
        // Search recursively for the ref element
        NodeList all = parent.getElementsByTagName(refTagName);
        if (all.getLength() > 0) {
            String text = all.item(0).getTextContent();
            return (text != null && !text.isBlank()) ? text.trim() : null;
        }
        // Try with namespace-agnostic local name search
        NodeList children = parent.getElementsByTagNameNS("*", refTagName);
        if (children.getLength() > 0) {
            String text = children.item(0).getTextContent();
            return (text != null && !text.isBlank()) ? text.trim() : null;
        }
        return null;
    }

    private static List<Element> findElements(Element root, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = root.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element el) {
                result.add(el);
            }
        }
        return result;
    }

    private static List<Element> directChildElements(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element el && el.getTagName().equals(tagName)) {
                result.add(el);
            }
        }
        return result;
    }

    /** Runtime exception for ReqIF parsing failures. */
    public static class ReqifParseException extends RuntimeException {
        public ReqifParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
