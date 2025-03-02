package org.jsmart.zerocode.core.domain;

import java.util.List;

public class Retry {
    private Integer max;
    private Integer delay;
    private String strategy; // New field for "fixed" or "exponential" strategy
    private List<String> withSteps;

    public Integer getMax() {
        return max;
    }

    public Integer getDelay() {
        return delay;
    }

    public String getStrategy() {
        return strategy;
    }

    public List<String> getWithSteps() {
        return withSteps;
    }
    public Retry() {}

    public Retry(Integer max, Integer delay, String strategy, List<String> withSteps) {
        this.max = max;
        this.delay = delay;
        this.strategy = strategy;
        this.withSteps = withSteps;
    }
}
