package hudson.plugins.logparser;

import hudson.Functions;
import jenkins.model.Jenkins;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class LogParserWriter {

    public static void writeHeaderTemplateToAllLinkFiles(
            final HashMap<String, BufferedWriter> writers,
            final int sectionCounter) throws IOException {
        final List<String> statuses = LogParserConsts.STATUSES_WITH_SECTIONS_IN_LINK_FILES;
        for (String status : statuses) {
            final BufferedWriter linkWriter = writers.get(status);
            String str = "HEADER HERE: #" + sectionCounter;
            linkWriter.write(str + "\n");
        }

    }

    public static void writeWrapperHtml(final String buildWrapperPath)
            throws IOException {
        final String wrapperHtml = "<frameset cols=\"270,*\">\n"
                + "<frame src=\"log_ref.html\" scrolling=auto name=\"sidebar\">\n"
                + "<frame src=\"log_content.html\" scrolling=auto name=\"content\">\n"
                + "<noframes>\n"
                + "<p>Viewing the build report requires a Frames-enabled browser</p>\n"
                + "<a href='build.log'>build log</a>\n" + "</noframes>\n"
                + "</frameset>\n";

        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(buildWrapperPath))) {
            writer.write(wrapperHtml);
        }
    }

    public static void writeReferenceHtml(final String buildRefPath,
            final ArrayList<String> headerForSection,
            final HashMap<String, Integer> statusCountPerSection,
            final HashMap<String, String> iconTable,
            final HashMap<String, String> linkListDisplay,
            final HashMap<String, String> linkListDisplayPlural,
            final HashMap<String, Integer> statusCount,
            final HashMap<String, String> linkFiles,
            final List<String> extraTags) throws IOException {

        final String refStart = "<script type=\"text/javascript\">\n"
                + "\tfunction toggleList(list){\n"
                + "\t\telement = document.getElementById(list).style;\n"
                + "\t\telement.display == 'none' ? element.display='block' : element.display='none';\n"
                + "\t}\n" + "</script>\n";

        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(buildRefPath))) {
            // Hudson stylesheets
            writer.write(LogParserConsts.getHtmlOpeningTags());
            writer.write(refStart); // toggle links javascript
            // Write Errors
            writeLinks(writer, LogParserConsts.ERROR, headerForSection,
                    statusCountPerSection, iconTable, linkListDisplay,
                    linkListDisplayPlural, statusCount, linkFiles);
            // Write Warnings
            writeLinks(writer, LogParserConsts.WARNING, headerForSection,
                    statusCountPerSection, iconTable, linkListDisplay,
                    linkListDisplayPlural, statusCount, linkFiles);
            // Write Infos
            writeLinks(writer, LogParserConsts.INFO, headerForSection,
                    statusCountPerSection, iconTable, linkListDisplay,
                    linkListDisplayPlural, statusCount, linkFiles);
            // Write Debugs
            writeLinks(writer, LogParserConsts.DEBUG, headerForSection,
                    statusCountPerSection, iconTable, linkListDisplay,
                    linkListDisplayPlural, statusCount, linkFiles);
            // Write extra tags
            for (String extraTag : extraTags) {
                writeLinks(writer, extraTag, headerForSection,
                        statusCountPerSection, iconTable, linkListDisplay,
                        linkListDisplayPlural, statusCount, linkFiles);
            }
            writer.write(LogParserConsts.getHtmlClosingTags());
        }
    }

    private static void writeLinks(final BufferedWriter writer,
            final String status, final ArrayList<String> headerForSection,
            final HashMap<String, Integer> statusCountPerSection,
            final HashMap<String, String> iconTable,
            final HashMap<String, String> linkListDisplay,
            final HashMap<String, String> linkListDisplayPlural,
            final HashMap<String, Integer> statusCount,
            final HashMap<String, String> linkFiles) throws IOException {
        String statusIcon = iconTable.get(status);
        if (statusIcon == null) {
            statusIcon = LogParserDisplayConsts.DEFAULT_ICON;
        }
        String linkListDisplayStr = linkListDisplay.get(status);
        if (linkListDisplayStr == null) {
            linkListDisplayStr = LogParserDisplayConsts.getDefaultLinkListDisplay(status);
        }
        String linkListDisplayStrPlural = linkListDisplayPlural.get(status);
        if (linkListDisplayStrPlural == null) {
            linkListDisplayStrPlural = LogParserDisplayConsts.getDefaultLinkListDisplayPlural(status);
        }
        final String linkListCount = statusCount.get(status).toString();

        final String hudsonRoot = Jenkins.get().getRootUrl();
        final String iconLocation = String.format("%s/plugin/log-parser/images/", jenkins.model.Jenkins.RESOURCE_PATH).substring(1);
		
        final String styles = 
            "<style>\n" 
            + "    ul {margin-left: 0; padding-left: 1em;}\n"
            + "    ul li {font-size: small; white-space: nowrap; text-overflow: ellipsis; overflow: hidden; margin-top: .5em; }\n"
            + "    ul li:hover {white-space: normal;}\n"
            + "    ul li a:link {text-decoration: none;}\n"
            + "    ul li:hover a:link {text-decoration: underline;}\n"
            + "</style>\n";
        writer.write(styles);
		
        final String linksStart = "<img src=\"" + hudsonRoot + iconLocation + statusIcon
                + "\" style=\"margin: 2px;\" width=\"24\" alt=\"" + linkListDisplayStr + " Icon\" height=\"24\" />\n"
                + "<a href=\"javascript:toggleList('" + linkListDisplayStr + "')\" target=\"_self\"><STRONG>"
                + linkListDisplayStr + " (" + linkListCount + ")</STRONG></a><br />\n"
                + "<ul style=\"display: none;\" id=\""
                + linkListDisplayStr + "\" >\n";
        writer.write(linksStart);

        // Read the links file and insert here
        try (final BufferedReader reader = new BufferedReader(new FileReader(linkFiles.get(status)))) {
            final String summaryLine = "<br/>(SUMMARY_INT_HERE LINK_LIST_DISPLAY_STR in this section)<br/>";

            final String headerTemplateRegexp = "HEADER HERE:";
            final String headerTemplateSplitBy = "#";

            // If it's a header line - put the header of the section
            String line;
            while ((line = reader.readLine()) != null) {
                String curSummaryLine = null;
                if (line.startsWith(headerTemplateRegexp)) {
                    final int headerNum = Integer.parseInt(line.split(headerTemplateSplitBy)[1]);
                    line = headerForSection.get(headerNum);
                    final String key = LogParserUtils.getSectionCountKey(status, headerNum);
                    final Integer summaryInt = statusCountPerSection.get(key);
                    if (summaryInt == null || summaryInt == 0) {
                        // Don't write the header if there are no relevant lines for
                        // this section
                        line = null;
                    } else {
                        String linkListDisplayStrWithPlural = linkListDisplayStr;
                        if (summaryInt > 1) {
                            linkListDisplayStrWithPlural = linkListDisplayStrPlural;
                        }
                        curSummaryLine = summaryLine.replace("SUMMARY_INT_HERE",
                                summaryInt.toString()).replace(
                                "LINK_LIST_DISPLAY_STR",
                                linkListDisplayStrWithPlural);
                    }

                }

                if (line != null) {
                    writer.write(line);
                    writer.newLine(); // Write system dependent end of line.
                }
                if (curSummaryLine != null) {
                    writer.write(curSummaryLine);
                    writer.newLine(); // Write system dependent end of line.
                }
            }
        }

        final String linksEnd = "</ul>\n";
        writer.write(linksEnd);

    }

    private LogParserWriter() {
        // PMD warning to use singleton or bypass by private empty constructor
    }

}
