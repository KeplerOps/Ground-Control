package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure Java parser for StrictDoc (.sdoc) requirement files.
 *
 * <p>Returns a structured {@link SdocDocument} containing sections with ordered
 * content items (requirement references and text blocks), plus a flat requirements
 * list for backward-compatible import pipelines.
 */
public final class SdocParser {

    private static final Pattern SECTION_START = Pattern.compile("\\[\\[SECTION]]");
    private static final Pattern SECTION_END = Pattern.compile("\\[\\[/SECTION]]");
    private static final Pattern REQUIREMENT_MARKER = Pattern.compile("\\[REQUIREMENT]");
    private static final Pattern TEXT_MARKER = Pattern.compile("\\[TEXT]");
    private static final Pattern UID_RE = Pattern.compile("^UID:\\s*([^\\n]++)$", Pattern.MULTILINE);
    private static final Pattern TITLE_RE = Pattern.compile("^TITLE:\\s*([^\\n]++)$", Pattern.MULTILINE);
    private static final Pattern STATEMENT_RE = Pattern.compile("STATEMENT: >>>\\n([\\s\\S]*?)\\n<<<");
    private static final Pattern COMMENT_RE = Pattern.compile("^COMMENT:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern WAVE_RE = Pattern.compile("Wave\\s+(\\d+)");
    private static final Pattern PARENT_RE = Pattern.compile("- TYPE: Parent\\s+VALUE:\\s*(\\S+)");
    private static final Pattern ISSUE_REF_RE = Pattern.compile("#(\\d+)");

    private static final List<String> BLOCK_TERMINATORS =
            List.of("[REQUIREMENT]", "[TEXT]", "[[SECTION]]", "[[/SECTION]]");

    private SdocParser() {}

    public static SdocDocument parse(String text) {
        List<Marker> markers = findAllMarkers(text);
        List<SdocSection> sections = new ArrayList<>();
        List<SdocRequirement> allRequirements = new ArrayList<>();

        String currentSectionTitle = null;
        Integer currentWave = null;
        List<SdocContentItem> currentItems = new ArrayList<>();

        for (int i = 0; i < markers.size(); i++) {
            Marker marker = markers.get(i);
            int nextMarkerOffset = (i + 1 < markers.size()) ? markers.get(i + 1).offset : text.length();

            switch (marker.type) {
                case SECTION_START -> {
                    if (currentSectionTitle != null) {
                        sections.add(new SdocSection(currentSectionTitle, currentWave, currentItems));
                        currentItems = new ArrayList<>();
                    }
                    String headerArea = text.substring(marker.offset, Math.min(marker.offset + 300, nextMarkerOffset));
                    currentSectionTitle = extractSectionTitle(headerArea);
                    currentWave = extractWaveNumber(headerArea);
                }
                case SECTION_END -> {
                    if (currentSectionTitle != null) {
                        sections.add(new SdocSection(currentSectionTitle, currentWave, currentItems));
                        currentItems = new ArrayList<>();
                        currentSectionTitle = null;
                        currentWave = null;
                    }
                }
                case REQUIREMENT -> {
                    String block = extractBlock(text, marker.endOffset, nextMarkerOffset);
                    SdocRequirement req = parseRequirementBlock(block, currentWave);
                    if (req != null) {
                        allRequirements.add(req);
                        currentItems.add(new SdocContentItem.RequirementRef(req.uid()));
                    }
                }
                case TEXT -> {
                    String block = extractBlock(text, marker.endOffset, nextMarkerOffset);
                    String content = block.strip();
                    if (!content.isEmpty()) {
                        currentItems.add(new SdocContentItem.TextBlock(content));
                    }
                }
                default -> throw new IllegalStateException("Unknown marker type: " + marker.type);
            }
        }

        if (currentSectionTitle != null) {
            sections.add(new SdocSection(currentSectionTitle, currentWave, currentItems));
        }

        return new SdocDocument(sections, allRequirements);
    }

    private static List<Marker> findAllMarkers(String text) {
        List<Marker> markers = new ArrayList<>();
        addMarkers(markers, SECTION_START, text, MarkerType.SECTION_START);
        addMarkers(markers, SECTION_END, text, MarkerType.SECTION_END);
        addMarkers(markers, REQUIREMENT_MARKER, text, MarkerType.REQUIREMENT);
        addMarkers(markers, TEXT_MARKER, text, MarkerType.TEXT);
        markers.sort((a, b) -> Integer.compare(a.offset, b.offset));
        return markers;
    }

    private static void addMarkers(List<Marker> markers, Pattern pattern, String text, MarkerType type) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            markers.add(new Marker(type, matcher.start(), matcher.end()));
        }
    }

    private static String extractBlock(String text, int blockStart, int nextMarkerOffset) {
        return text.substring(blockStart, nextMarkerOffset);
    }

    private static String extractSectionTitle(String headerArea) {
        Matcher titleMatcher = TITLE_RE.matcher(headerArea);
        return titleMatcher.find() ? titleMatcher.group(1).strip() : "";
    }

    private static Integer extractWaveNumber(String headerArea) {
        Matcher titleMatcher = TITLE_RE.matcher(headerArea);
        if (titleMatcher.find()) {
            Matcher waveMatcher = WAVE_RE.matcher(titleMatcher.group(1));
            if (waveMatcher.find()) {
                return Integer.parseInt(waveMatcher.group(1));
            }
        }
        return null;
    }

    private static SdocRequirement parseRequirementBlock(String block, Integer wave) {
        Matcher uidMatcher = UID_RE.matcher(block);
        String uid = uidMatcher.find() ? uidMatcher.group(1).strip() : null;
        if (uid == null) {
            return null;
        }

        Matcher titleMatcher = TITLE_RE.matcher(block);
        String title = titleMatcher.find() ? titleMatcher.group(1).strip() : "";

        Matcher statementMatcher = STATEMENT_RE.matcher(block);
        String statement = statementMatcher.find() ? statementMatcher.group(1).strip() : "";

        Matcher commentMatcher = COMMENT_RE.matcher(block);
        String comment = commentMatcher.find() ? commentMatcher.group(1).strip() : "";

        List<Integer> issueRefs = extractIssueRefs(comment);
        List<String> parentUids = extractParentUids(block);

        return new SdocRequirement(uid, title, statement, comment, issueRefs, parentUids, wave);
    }

    private static List<Integer> extractIssueRefs(String comment) {
        if (comment.isEmpty()) {
            return List.of();
        }
        List<Integer> issueRefs = new ArrayList<>();
        Matcher issueMatcher = ISSUE_REF_RE.matcher(comment);
        while (issueMatcher.find()) {
            issueRefs.add(Integer.parseInt(issueMatcher.group(1)));
        }
        return issueRefs;
    }

    private static List<String> extractParentUids(String block) {
        List<String> parentUids = new ArrayList<>();
        Matcher parentMatcher = PARENT_RE.matcher(block);
        while (parentMatcher.find()) {
            parentUids.add(parentMatcher.group(1));
        }
        return parentUids;
    }

    private enum MarkerType {
        SECTION_START,
        SECTION_END,
        REQUIREMENT,
        TEXT
    }

    private record Marker(MarkerType type, int offset, int endOffset) {}
}
