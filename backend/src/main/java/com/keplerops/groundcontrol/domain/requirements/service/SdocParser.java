package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure Java parser for StrictDoc (.sdoc) requirement files.
 *
 * <p>Ports the Python reference implementation at
 * {@code archive/tools/issue-graph/issue_graph.py:parse_sdoc()}.
 */
public final class SdocParser {

    private static final Pattern SECTION_START = Pattern.compile("\\[\\[SECTION]]");
    private static final Pattern SECTION_END = Pattern.compile("\\[\\[/SECTION]]");
    private static final Pattern REQUIREMENT_MARKER = Pattern.compile("\\[REQUIREMENT]");
    private static final Pattern UID_RE = Pattern.compile("^UID:\\s*([^\\n]++)$", Pattern.MULTILINE);
    private static final Pattern TITLE_RE = Pattern.compile("^TITLE:\\s*([^\\n]++)$", Pattern.MULTILINE);
    private static final Pattern STATEMENT_RE = Pattern.compile("STATEMENT: >>>\\n([\\s\\S]*?)\\n<<<");
    private static final Pattern COMMENT_RE = Pattern.compile("^COMMENT:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern WAVE_RE = Pattern.compile("Wave\\s+(\\d+)");
    private static final Pattern PARENT_RE = Pattern.compile("- TYPE: Parent\\s+VALUE:\\s*(\\S+)");
    private static final Pattern ISSUE_REF_RE = Pattern.compile("#(\\d+)");

    private SdocParser() {}

    public static List<SdocRequirement> parse(String text) {
        List<WaveRange> waveRanges = buildWaveRanges(text);
        return parseRequirementBlocks(text, waveRanges);
    }

    private static List<WaveRange> buildWaveRanges(String text) {
        List<int[]> sectionStarts = collectPositions(SECTION_START, text);
        List<int[]> sectionEnds = collectPositions(SECTION_END, text);

        List<WaveRange> waveRanges = new ArrayList<>();
        for (int i = 0; i < sectionStarts.size(); i++) {
            int start = sectionStarts.get(i)[0];
            int end = i < sectionEnds.size() ? sectionEnds.get(i)[0] : text.length();
            Integer wave = extractWaveFromSection(text, start);
            waveRanges.add(new WaveRange(start, end, wave));
        }
        return waveRanges;
    }

    private static Integer extractWaveFromSection(String text, int sectionStart) {
        int searchEnd = Math.min(sectionStart + 200, text.length());
        String titleSearchArea = text.substring(sectionStart, searchEnd);
        Matcher titleMatcher = TITLE_RE.matcher(titleSearchArea);
        if (titleMatcher.find()) {
            Matcher waveMatcher = WAVE_RE.matcher(titleMatcher.group(1));
            if (waveMatcher.find()) {
                return Integer.parseInt(waveMatcher.group(1));
            }
        }
        return null;
    }

    private static List<SdocRequirement> parseRequirementBlocks(String text, List<WaveRange> waveRanges) {
        List<SdocRequirement> requirements = new ArrayList<>();
        Matcher reqMatcher = REQUIREMENT_MARKER.matcher(text);

        while (reqMatcher.find()) {
            String block = extractBlock(text, reqMatcher.end());
            SdocRequirement req = parseBlock(block, waveRanges, reqMatcher.start());
            if (req != null) {
                requirements.add(req);
            }
        }

        return Collections.unmodifiableList(requirements);
    }

    private static String extractBlock(String text, int blockStart) {
        int blockEnd = text.length();
        for (String marker : List.of("[REQUIREMENT]", "[TEXT]", "[[SECTION]]", "[[/SECTION]]")) {
            int idx = text.indexOf(marker, blockStart);
            if (idx != -1 && idx < blockEnd) {
                blockEnd = idx;
            }
        }
        return text.substring(blockStart, blockEnd);
    }

    private static SdocRequirement parseBlock(String block, List<WaveRange> waveRanges, int reqOffset) {
        Matcher uidMatcher = UID_RE.matcher(block);
        String uid = uidMatcher.find() ? uidMatcher.group(1).strip() : null;
        if (uid == null) {
            return null;
        }

        Matcher blockTitleMatcher = TITLE_RE.matcher(block);
        String title = blockTitleMatcher.find() ? blockTitleMatcher.group(1).strip() : "";

        Matcher statementMatcher = STATEMENT_RE.matcher(block);
        String statement = statementMatcher.find() ? statementMatcher.group(1).strip() : "";

        Matcher commentMatcher = COMMENT_RE.matcher(block);
        String comment = commentMatcher.find() ? commentMatcher.group(1).strip() : "";

        List<Integer> issueRefs = extractIssueRefs(comment);
        List<String> parentUids = extractParentUids(block);
        Integer wave = waveForOffset(waveRanges, reqOffset);

        return new SdocRequirement(
                uid,
                title,
                statement,
                comment,
                Collections.unmodifiableList(issueRefs),
                Collections.unmodifiableList(parentUids),
                wave);
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

    private static Integer waveForOffset(List<WaveRange> waveRanges, int offset) {
        for (WaveRange range : waveRanges) {
            if (range.start <= offset && offset <= range.end) {
                return range.wave;
            }
        }
        return null;
    }

    private static List<int[]> collectPositions(Pattern pattern, String text) {
        List<int[]> positions = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            positions.add(new int[] {matcher.start()});
        }
        return positions;
    }

    private record WaveRange(int start, int end, Integer wave) {}
}
