package com.kkamar.cnh.support;

import com.kkamar.cnh.dto.CrawlAlert;
import com.kkamar.cnh.service.AlertNotifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 보내진 알림을 메모리에 기록하는 테스트용 알림 채널. */
public class RecordingAlertNotifier implements AlertNotifier {

    private final List<CrawlAlert> sent = new CopyOnWriteArrayList<>();
    private volatile boolean deliverable = true;

    @Override
    public boolean send(CrawlAlert alert) {
        if (!deliverable) {
            return false;
        }
        sent.add(alert);
        return true;
    }

    public List<CrawlAlert> sent() {
        return sent;
    }

    /** false로 두면 전달 실패(Discord 장애 등)를 흉내 낸다. */
    public void setDeliverable(boolean deliverable) {
        this.deliverable = deliverable;
    }

    public void reset() {
        sent.clear();
        deliverable = true;
    }
}
