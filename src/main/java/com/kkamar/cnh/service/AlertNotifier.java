package com.kkamar.cnh.service;

import com.kkamar.cnh.dto.CrawlAlert;

/** 운영자에게 크롤러 이상을 알리는 채널. */
public interface AlertNotifier {

    /**
     * @return 알림이 실제로 전달되었으면 true. false면 다음 사이클에 같은 알림을 다시 시도한다.
     */
    boolean send(CrawlAlert alert);
}
