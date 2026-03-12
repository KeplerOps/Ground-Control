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
    private static final Pattern UID_RE = Pattern.compile("^UID:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern TITLE_RE = Pattern.compile("^TITLE:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern STATEMENT_RE = Pattern.compile("STATEMENT: >>>\\n(.*?)\\n<<<", Pattern.DOTALL);
    private static final Pattern COMMENT_RE = Pattern.compile("^COMMENT:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern WAVE_RE = Pattern.compile("Wave\\s+(\\d+)");
    private static final Pattern PARENT_RE = Pattern.compile("- TYPE: Parent\\s+VALUE:\\s*(\\S+)");
    private static final Pattern ISSUE_REF_RE = Pattern.compile("#(\\d+)");

    private SdocParser() {}

    public static List<SdocRequirement> parse(String text) {
        // Phase 1: Build wave ranges from section markers
        List<int[]> sectionStarts = collectPositions(SECTION_START, text);
        List<int[]> sectionEnds = collectPositions(SECTION_END, text);

        List<WaveRange> waveRanges = new ArrayList<>();
        for (int i = 0; i < sectionStarts.size(); i++) {
            int start = sectionStarts.get(i)[0];
            int end = i < sectionEnds.size() ? sectionEnds.get(i)[0] : text.length();

            // Find TITLE within 200 chars of section start
            int searchEnd = Math.min(start + 200, text.length());
            String titleSearchArea = text.substring(start, searchEnd);
            Matcher titleMatcher = TITLE_RE.matcher(titleSearchArea);

            Integer wave = null;
            if (titleMatcher.find()) {
                Matcher waveMatcher = WAVE_RE.matcher(titleMatcher.group(1));
                if (waveMatcher.find()) {
                    wave = Integer.parseInt(waveMatcher.group(1));
                }
            }
            waveRanges.add(new WaveRange(start, end, wave));
        }

        // Phase 2: Parse requirement blocks
        List<SdocRequirement> requirements = new ArrayList<>();
        Matcher reqMatcher = REQUIREMENT_MARKER.matcher(text);

        while (reqMatcher.find()) {
            int blockStart = reqMatcher.end();
            int blockEnd = text.length();

            // Find end of this block (next marker of any type)
            for (String marker : List.of("[REQUIREMENT]", "[TEXT]", "[[SECTION]]", "[[/SECTION]]")) {
                int idx = text.indexOf(marker, blockStart);
                if (idx != -1 && idx < blockEnd) {
                    blockEnd = idx;
                }
            }
            String block = text.substring(blockStart, blockEnd);

            Matcher uidMatcher = UID_RE.matcher(block);
            String uid = uidMatcher.find() ? uidMatcher.group(1).strip() : null;

            if (uid == null) {
                continue;
            }

            Matcher blockTitleMatcher = TITLE_RE.matcher(block);
            String title = blockTitleMatcher.find() ? blockTitleMatcher.group(1).strip() : "";

            Matcher statementMatcher = STATEMENT_RE.matcher(block);
            String statement =
                    statementMatcher.find() ? statementMatcher.group(1).strip() : "";

            Matcher commentMatcher = COMMENT_RE.matcher(block);
            String comment = commentMatcher.find() ? commentMatcher.group(1).strip() : "";

            // Extract issue numbers from comment
            List<Integer> issueRefs = new ArrayList<>();
            if (!comment.isEmpty()) {
                Matcher issueMatcher = ISSUE_REF_RE.matcher(comment);
                while (issueMatcher.find()) {
                    issueRefs.add(Integer.parseInt(issueMatcher.group(1)));
                }
            }

            // Extract parent relations
            List<String> parentUids = new ArrayList<>();
            Matcher parentMatcher = PARENT_RE.matcher(block);
            while (parentMatcher.find()) {
                parentUids.add(parentMatcher.group(1));
            }

            // Determine wave from section context
            Integer wave = waveForOffset(waveRanges, reqMatcher.start());

            requirements.add(new SdocRequirement(
                    uid,
                    title,
                    statement,
                    comment,
                    Collections.unmodifiableList(issueRefs),
                    Collections.unmodifiableList(parentUids),
                    wave));
        }

        return Collections.unmodifiableList(requirements);
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
