package com.kkamar.cnh.util;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 게시판 HTML -> 공지 목록 파싱 로직.
 * 네트워크/DB에 의존하지 않는 순수 함수로 분리해 두었으므로,
 * 저장해 둔 HTML(스냅샷)만으로 파서 회귀 테스트를 돌릴 수 있다.
 *
 * rowsFound      : 문서 안의 모든 &lt;tr&gt; 개수 (표 머리글 행 포함)
 * linksExtracted : 그중 제목/링크 파싱에 성공한 행 개수 (= notices.size())
 *                  DB에 이미 있는 글이어도 파싱에 성공하면 포함되므로 "신규 공지 수"가 아니다.
 */
public final class BoardHtmlParser {

    private static final Logger log = LoggerFactory.getLogger(BoardHtmlParser.class);

    private static final Pattern COMB_BBS_PATTERN =
            Pattern.compile("jf_combBbs_view\\('([^']+)','([^']+)','([^']+)','([^']+)'\\)");
    private static final Pattern EXTERNAL_ID_FROM_URL_PATTERN =
            Pattern.compile("/(\\d+)/artclView\\.do");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public record ParsedNotice(String externalId, String title, String detailUrl, LocalDate postedAt) {
    }

    public record ParseResult(int rowsFound, List<ParsedNotice> notices) {
        public int linksExtracted() {
            return notices.size();
        }
    }

    private record LinkInfo(String externalId, String detailUrl) {
    }

    private BoardHtmlParser() {
    }

    /** 학교 전체(통합) 공지 게시판: href가 javascript:jf_combBbs_view(...) 형태 */
    public static ParseResult parseUniversityBoard(Document doc, String portalBaseUrl) {
        Elements rows = doc.select("tr");
        List<ParsedNotice> notices = new ArrayList<>();

        for (Element row : rows) {
            Element titleEl = row.selectFirst("td.td-subject a");
            if (titleEl == null) continue;

            String title = titleEl.text().trim();
            LinkInfo linkInfo = extractLinkInfo(portalBaseUrl, titleEl);
            if (linkInfo == null) {
                log.warn("[University board] failed to extract link, href='{}'", titleEl.attr("href"));
                continue;
            }
            notices.add(new ParsedNotice(linkInfo.externalId(), title, linkInfo.detailUrl(), extractDate(row)));
        }
        return new ParseResult(rows.size(), notices);
    }

    /** 학과 게시판: href가 .../{id}/artclView.do 형태의 일반 링크 */
    public static ParseResult parseDepartmentBoard(Document doc) {
        Elements rows = doc.select("tr");
        List<ParsedNotice> notices = new ArrayList<>();

        for (Element row : rows) {
            Element titleEl = row.selectFirst("td.td-subject a");
            if (titleEl == null) continue;

            Element strongEl = row.selectFirst("td.td-subject a strong");
            String title = strongEl != null ? strongEl.text().trim() : titleEl.text().trim();
            String detailUrl = titleEl.absUrl("href");

            String externalId = extractExternalIdFromUrl(detailUrl);
            if (externalId == null) {
                log.warn("[Department board] failed to extract external id, url='{}'", detailUrl);
                continue;
            }
            notices.add(new ParsedNotice(externalId, title, detailUrl, extractDate(row)));
        }
        return new ParseResult(rows.size(), notices);
    }

    private static String extractExternalIdFromUrl(String url) {
        Matcher m = EXTERNAL_ID_FROM_URL_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private static LinkInfo extractLinkInfo(String portalBaseUrl, Element titleEl) {
        String href = titleEl.attr("href");

        if (href.startsWith("javascript:")) {
            Matcher m = COMB_BBS_PATTERN.matcher(href);
            if (!m.find()) {
                return null;
            }
            String site = m.group(1);
            String bbsId = m.group(3);
            String nttId = m.group(4);
            String base = portalBaseUrl == null ? "" : portalBaseUrl;
            String detailUrl = base + "/bbs/" + site + "/" + bbsId + "/" + nttId + "/artclView.do";
            return new LinkInfo(nttId, detailUrl);
        }

        String detailUrl = titleEl.absUrl("href");
        String externalId = extractExternalIdFromUrl(detailUrl);
        if (externalId == null) externalId = href; // fallback: URL에서 숫자 id를 못 찾은 경우
        return new LinkInfo(externalId, detailUrl);
    }

    private static LocalDate extractDate(Element row) {
        Element dateEl = row.selectFirst("td.td-date");
        if (dateEl == null) return null;
        String raw = dateEl.text().trim().replaceAll("\\.$", "");
        try {
            return LocalDate.parse(raw, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}
