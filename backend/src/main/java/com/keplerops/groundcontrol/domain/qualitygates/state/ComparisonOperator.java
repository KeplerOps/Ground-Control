package com.keplerops.groundcontrol.domain.qualitygates.state;

public enum ComparisonOperator {
    GTE,
    LTE,
    EQ,
    GT,
    LT;

    public boolean evaluate(double actual, double threshold) {
        return switch (this) {
            case GTE -> actual >= threshold;
            case LTE -> actual <= threshold;
            case EQ -> Double.compare(actual, threshold) == 0;
            case GT -> actual > threshold;
            case LT -> actual < threshold;
        };
    }
}
