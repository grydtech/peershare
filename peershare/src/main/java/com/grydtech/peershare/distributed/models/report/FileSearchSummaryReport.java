package com.grydtech.peershare.distributed.models.report;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class FileSearchSummaryReport {

    @JsonIgnore
    private long responsesCount = 0;

    private long averageSuccessResponseTime = 0;
    private long minSuccessResponseTime = 0;
    private long maxSuccessResponseTime = 0;

    public long getAverageSuccessResponseTime() {
        return averageSuccessResponseTime;
    }

    public void setAverageSuccessResponseTime(long averageSuccessResponseTime) {
        this.averageSuccessResponseTime = averageSuccessResponseTime;
    }

    public long getMinSuccessResponseTime() {
        return minSuccessResponseTime;
    }

    public void submitSuccessResponseTime(long time) {
        if (time == -1) return;

        minSuccessResponseTime = (minSuccessResponseTime > time) ? time : minSuccessResponseTime;
        maxSuccessResponseTime = (maxSuccessResponseTime < time) ? time : maxSuccessResponseTime;

        averageSuccessResponseTime = (averageSuccessResponseTime * responsesCount + time) / responsesCount + 1;

        responsesCount++;
    }
}
