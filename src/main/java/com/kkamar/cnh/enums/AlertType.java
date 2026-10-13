package com.kkamar.cnh.enums;

/**
 * 한 사이클에서 실제로 발송된(또는 발송해야 하는) 알림의 종류.
 * INCIDENT  : 이상이 처음 확정되어 보내는 알림
 * REMINDER  : 이상이 계속되는 동안 주기적으로 보내는 재알림
 * RECOVERED : 이상 후 정상으로 돌아왔을 때 보내는 복구 알림
 */
public enum AlertType {
    NONE,
    INCIDENT,
    REMINDER,
    RECOVERED;

    /** 이상 상황을 알리는 종류인지 (복구 알림 제외) */
    public boolean isFiring() {
        return this == INCIDENT || this == REMINDER;
    }
}
