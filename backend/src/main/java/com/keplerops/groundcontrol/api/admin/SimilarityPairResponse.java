package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.SimilarityPair;

public record SimilarityPairResponse(String uid1, String title1, String uid2, String title2, double score) {

    public static SimilarityPairResponse from(SimilarityPair pair) {
        return new SimilarityPairResponse(pair.uid1(), pair.title1(), pair.uid2(), pair.title2(), pair.score());
    }
}
