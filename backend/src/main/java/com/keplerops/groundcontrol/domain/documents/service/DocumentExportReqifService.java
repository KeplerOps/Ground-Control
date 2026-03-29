package com.keplerops.groundcontrol.domain.documents.service;

import com.keplerops.groundcontrol.domain.requirements.service.ExportException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.springframework.stereotype.Service;

/** Serializes a document's reading order to ReqIF 1.2 XML format. */
@Service
public class DocumentExportReqifService {

    private static final String REQIF_NS = "http://www.omg.org/spec/ReqIF/20110401/reqif.xsd";
    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

    public String toReqif(DocumentReadingOrder order, Map<String, RequirementExportData> requirementsByUid) {
        try {
            var sw = new StringWriter();
            var factory = XMLOutputFactory.newInstance();
            var xml = factory.createXMLStreamWriter(sw);

            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("REQ-IF");
            xml.writeDefaultNamespace(REQIF_NS);

            writeHeader(xml, order);
            writeCoreContent(xml, order, requirementsByUid);

            xml.writeEndElement(); // REQ-IF
            xml.writeEndDocument();
            xml.flush();
            return sw.toString();
        } catch (XMLStreamException e) {
            throw new ExportException("Failed to generate ReqIF export", e);
        }
    }

    private void writeHeader(XMLStreamWriter xml, DocumentReadingOrder order) throws XMLStreamException {
        xml.writeStartElement("THE-HEADER");
        xml.writeStartElement("REQ-IF-HEADER");
        xml.writeAttribute("IDENTIFIER", "header-" + order.documentId());
        writeTextElement(xml, "TITLE", nullSafe(order.title()));
        xml.writeEndElement(); // REQ-IF-HEADER
        xml.writeEndElement(); // THE-HEADER
    }

    private void writeCoreContent(
            XMLStreamWriter xml, DocumentReadingOrder order, Map<String, RequirementExportData> requirementsByUid)
            throws XMLStreamException {
        xml.writeStartElement("CORE-CONTENT");
        xml.writeStartElement("REQ-IF-CONTENT");

        writeDatatypes(xml);
        writeSpecTypes(xml);
        writeSpecObjects(xml, requirementsByUid);
        writeSpecRelations(xml, requirementsByUid);
        writeSpecifications(xml, order, requirementsByUid);

        xml.writeEndElement(); // REQ-IF-CONTENT
        xml.writeEndElement(); // CORE-CONTENT
    }

    private void writeDatatypes(XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("DATATYPES");
        xml.writeEmptyElement("DATATYPE-DEFINITION-STRING");
        xml.writeAttribute("IDENTIFIER", "dt-string");
        xml.writeAttribute("LONG-NAME", "String");
        xml.writeAttribute("MAX-LENGTH", "4000");
        xml.writeEmptyElement("DATATYPE-DEFINITION-XHTML");
        xml.writeAttribute("IDENTIFIER", "dt-xhtml");
        xml.writeAttribute("LONG-NAME", "XHTML");
        xml.writeEndElement(); // DATATYPES
    }

    private void writeSpecTypes(XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("SPEC-TYPES");

        // SPEC-OBJECT-TYPE for requirements
        xml.writeStartElement("SPEC-OBJECT-TYPE");
        xml.writeAttribute("IDENTIFIER", "sot-requirement");
        xml.writeAttribute("LONG-NAME", "Requirement");
        xml.writeStartElement("SPEC-ATTRIBUTES");
        writeAttrDefString(xml, "ad-name", "ReqIF.Name", "dt-string");
        writeAttrDefXhtml(xml, "ad-text", "ReqIF.Text", "dt-xhtml");
        xml.writeEndElement(); // SPEC-ATTRIBUTES
        xml.writeEndElement(); // SPEC-OBJECT-TYPE

        // SPEC-RELATION-TYPE for parent relations
        xml.writeStartElement("SPEC-RELATION-TYPE");
        xml.writeAttribute("IDENTIFIER", "srt-parent");
        xml.writeAttribute("LONG-NAME", "Parent Relationship");
        xml.writeEmptyElement("SPEC-ATTRIBUTES");
        xml.writeEndElement(); // SPEC-RELATION-TYPE

        xml.writeEndElement(); // SPEC-TYPES
    }

    private void writeAttrDefString(XMLStreamWriter xml, String id, String name, String typeRef)
            throws XMLStreamException {
        xml.writeStartElement("ATTRIBUTE-DEFINITION-STRING");
        xml.writeAttribute("IDENTIFIER", id);
        xml.writeAttribute("LONG-NAME", name);
        xml.writeStartElement("TYPE");
        writeTextElement(xml, "DATATYPE-DEFINITION-STRING-REF", typeRef);
        xml.writeEndElement(); // TYPE
        xml.writeEndElement(); // ATTRIBUTE-DEFINITION-STRING
    }

    private void writeAttrDefXhtml(XMLStreamWriter xml, String id, String name, String typeRef)
            throws XMLStreamException {
        xml.writeStartElement("ATTRIBUTE-DEFINITION-XHTML");
        xml.writeAttribute("IDENTIFIER", id);
        xml.writeAttribute("LONG-NAME", name);
        xml.writeStartElement("TYPE");
        writeTextElement(xml, "DATATYPE-DEFINITION-XHTML-REF", typeRef);
        xml.writeEndElement(); // TYPE
        xml.writeEndElement(); // ATTRIBUTE-DEFINITION-XHTML
    }

    private void writeSpecObjects(XMLStreamWriter xml, Map<String, RequirementExportData> requirementsByUid)
            throws XMLStreamException {
        xml.writeStartElement("SPEC-OBJECTS");
        for (var req : requirementsByUid.values()) {
            xml.writeStartElement("SPEC-OBJECT");
            xml.writeAttribute("IDENTIFIER", req.uid());
            xml.writeAttribute("LONG-NAME", nullSafe(req.title()));

            xml.writeStartElement("TYPE");
            writeTextElement(xml, "SPEC-OBJECT-TYPE-REF", "sot-requirement");
            xml.writeEndElement(); // TYPE

            xml.writeStartElement("VALUES");
            writeXhtmlValue(xml, req.statement());
            xml.writeEndElement(); // VALUES

            xml.writeEndElement(); // SPEC-OBJECT
        }
        xml.writeEndElement(); // SPEC-OBJECTS
    }

    private void writeXhtmlValue(XMLStreamWriter xml, String text) throws XMLStreamException {
        xml.writeStartElement("ATTRIBUTE-VALUE-XHTML");
        xml.writeStartElement("DEFINITION");
        writeTextElement(xml, "ATTRIBUTE-DEFINITION-XHTML-REF", "ad-text");
        xml.writeEndElement(); // DEFINITION
        xml.writeStartElement("THE-VALUE");
        xml.writeStartElement("xhtml", "div", XHTML_NS);
        xml.writeNamespace("xhtml", XHTML_NS);
        xml.writeCharacters(nullSafe(text));
        xml.writeEndElement(); // xhtml:div
        xml.writeEndElement(); // THE-VALUE
        xml.writeEndElement(); // ATTRIBUTE-VALUE-XHTML
    }

    private void writeSpecRelations(XMLStreamWriter xml, Map<String, RequirementExportData> requirementsByUid)
            throws XMLStreamException {
        xml.writeStartElement("SPEC-RELATIONS");
        int relIndex = 0;
        for (var req : requirementsByUid.values()) {
            for (String parentUid : req.parentUids()) {
                xml.writeStartElement("SPEC-RELATION");
                xml.writeAttribute("IDENTIFIER", "rel-" + (++relIndex));

                xml.writeStartElement("TYPE");
                writeTextElement(xml, "SPEC-RELATION-TYPE-REF", "srt-parent");
                xml.writeEndElement(); // TYPE

                xml.writeStartElement("SOURCE");
                writeTextElement(xml, "SOURCE-REF", req.uid());
                xml.writeEndElement(); // SOURCE

                xml.writeStartElement("TARGET");
                writeTextElement(xml, "TARGET-REF", parentUid);
                xml.writeEndElement(); // TARGET

                xml.writeEndElement(); // SPEC-RELATION
            }
        }
        xml.writeEndElement(); // SPEC-RELATIONS
    }

    private void writeSpecifications(
            XMLStreamWriter xml, DocumentReadingOrder order, Map<String, RequirementExportData> requirementsByUid)
            throws XMLStreamException {
        xml.writeStartElement("SPECIFICATIONS");
        xml.writeStartElement("SPECIFICATION");
        xml.writeAttribute("IDENTIFIER", "spec-" + order.documentId());
        xml.writeAttribute("LONG-NAME", nullSafe(order.title()));

        var counter = new AtomicInteger(0);
        xml.writeStartElement("CHILDREN");
        for (var section : order.sections()) {
            writeHierarchy(xml, section, requirementsByUid, counter);
        }
        xml.writeEndElement(); // CHILDREN

        xml.writeEndElement(); // SPECIFICATION
        xml.writeEndElement(); // SPECIFICATIONS
    }

    private void writeHierarchy(
            XMLStreamWriter xml,
            ReadingOrderNode section,
            Map<String, RequirementExportData> requirementsByUid,
            AtomicInteger counter)
            throws XMLStreamException {
        // Collect requirement UIDs in this section for the hierarchy
        List<String> reqUids = new ArrayList<>();
        for (var item : section.content()) {
            if ("REQUIREMENT".equals(item.contentType()) && item.requirementUid() != null) {
                reqUids.add(item.requirementUid());
            }
        }

        // Write each requirement as a SPEC-HIERARCHY node
        for (String uid : reqUids) {
            if (!requirementsByUid.containsKey(uid)) {
                continue;
            }
            xml.writeStartElement("SPEC-HIERARCHY");
            xml.writeAttribute("IDENTIFIER", "sh-" + counter.incrementAndGet());

            xml.writeStartElement("OBJECT");
            writeTextElement(xml, "OBJECT-REF", uid);
            xml.writeEndElement(); // OBJECT

            xml.writeEndElement(); // SPEC-HIERARCHY
        }

        // Recurse into child sections
        for (var child : section.children()) {
            writeHierarchy(xml, child, requirementsByUid, counter);
        }
    }

    private void writeTextElement(XMLStreamWriter xml, String name, String text) throws XMLStreamException {
        xml.writeStartElement(name);
        xml.writeCharacters(text);
        xml.writeEndElement();
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }
}
