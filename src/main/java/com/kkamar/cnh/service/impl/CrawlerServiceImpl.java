package com.kkamar.cnh.service.impl;

import com.kkamar.cnh.domain.Board;
import com.kkamar.cnh.domain.Notice;
import com.kkamar.cnh.repository.BoardRepository;
import com.kkamar.cnh.repository.NoticeRepository;
import com.kkamar.cnh.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerServiceImpl implements CrawlerService {

    private final BoardRepository boardRepository;
    private final NoticeRepository noticeRepository;

    private static final Pattern COMB_BBS_PATTERN =
            Pattern.compile("jf_combBbs_view\\('([^']+)','([^']+)','([^']+)','([^']+)'\\)");
    private static final Pattern EXTERNAL_ID_FROM_URL_PATTERN =
            Pattern.compile("/(\\d+)/artclView\\.do");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private record NoticeLinkInfo(String externalId, String detailUrl) {}

    @Override
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void crawlAllBoards() {
        List<Board> boards = boardRepository.findAll();
        log.info("Found {} board(s) to crawl", boards.size());

        for (Board board : boards) {
            log.info("Processing board: id={}, name='{}', departmentId={}",
                    board.getId(), board.getName(),
                    board.getDepartment() == null ? "null" : board.getDepartment().getId());
            try {
                if (board.getDepartment() == null) {
                    crawlAllNoticeBoard(board); // university-wide (combined) board
                } else {
                    crawlDeptNoticeBoard(board); // department board
                }
            } catch (Exception e) {
                log.error("Crawl failed: board={}", board.getName(), e);
            }
        }
    }

    private void crawlAllNoticeBoard(Board board) throws Exception {
        Document doc = Jsoup.connect(board.getCrawlUrl())
                .userAgent("Mozilla/5.0")
                .timeout(5000)
                .get();

        Elements rows = doc.select("tr");
        log.info("[All-notice board] rows found: {}", rows.size());

        int extracted = 0;
        for (Element row : rows) {
            Element titleEl = row.selectFirst("td.td-subject a");
            if (titleEl == null) continue;

            String title = titleEl.text().trim();
            NoticeLinkInfo linkInfo = extractLinkInfo(board, titleEl);
            if (linkInfo == null) {
                log.warn("[All-notice board] failed to extract link, href='{}'", titleEl.attr("href"));
                continue;
            }
            extracted++;

            saveIfNew(board, linkInfo.externalId(), title, linkInfo.detailUrl(), extractDate(row));
        }
        log.info("[All-notice board] links extracted: {}", extracted);
    }

    private void crawlDeptNoticeBoard(Board board) throws Exception {
        Document doc = Jsoup.connect(board.getCrawlUrl())
                .userAgent("Mozilla/5.0")
                .timeout(5000)
                .get();

        Elements rows = doc.select("tr");
        log.info("[Dept-notice board] rows found: {}", rows.size());

        for (Element row : rows) {
            Element titleEl = row.selectFirst("td.td-subject a");
            if (titleEl == null) continue;

            Element strongEl = row.selectFirst("td.td-subject a strong");
            String title = strongEl != null ? strongEl.text().trim() : titleEl.text().trim();
            String detailUrl = titleEl.absUrl("href");

            String externalId = extractExternalIdFromUrl(detailUrl);
            if (externalId == null) {
                log.warn("[Dept-notice board] failed to extract external id, url='{}'", detailUrl);
                continue;
            }

            saveIfNew(board, externalId, title, detailUrl, extractDate(row));
        }
    }

    private String extractExternalIdFromUrl(String url) {
        Matcher m = EXTERNAL_ID_FROM_URL_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private NoticeLinkInfo extractLinkInfo(Board board, Element titleEl) {
        String href = titleEl.attr("href");

        if (href.startsWith("javascript:")) {
            Matcher m = COMB_BBS_PATTERN.matcher(href);
            if (!m.find()) {
                return null;
            }
            String site = m.group(1);
            String bbsId = m.group(3);
            String nttId = m.group(4);
            String detailUrl = board.getUniversity().getPortalBaseUrl()
                    + "/bbs/" + site + "/" + bbsId + "/" + nttId + "/artclView.do";
            return new NoticeLinkInfo(nttId, detailUrl);
        }

        String detailUrl = titleEl.absUrl("href");
        String externalId = extractExternalIdFromUrl(detailUrl);
        if (externalId == null) externalId = href; // fallback: couldn't find a numeric id in the URL
        return new NoticeLinkInfo(externalId, detailUrl);
    }

    private LocalDate extractDate(Element row) {
        Element dateEl = row.selectFirst("td.td-date");
        if (dateEl == null) return null;
        String raw = dateEl.text().trim().replaceAll("\\.$", "");
        try {
            return LocalDate.parse(raw, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveIfNew(Board board, String externalId, String title, String url, LocalDate postedAt) {
        if (noticeRepository.existsByBoardIdAndExternalId(board.getId(), externalId)) {
            log.debug("Notice already exists, skipping: {}", title);
            return;
        }
        Notice notice = Notice.builder()
                .board(board)
                .externalId(externalId)
                .title(title)
                .originalUrl(url)
                .postedAt(postedAt)
                .build();
        noticeRepository.save(notice);
        log.info("Saved new notice: {}", title);
    }
}