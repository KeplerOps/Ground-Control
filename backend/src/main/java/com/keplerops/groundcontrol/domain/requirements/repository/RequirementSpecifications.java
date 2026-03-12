package com.keplerops.groundcontrol.domain.requirements.repository;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementFilter;
import com.keplerops.groundcontrol.domain.requirements.state.RequirementType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class RequirementSpecifications {

    private RequirementSpecifications() {}

    public static Specification<Requirement> hasStatus(Status status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Requirement> hasRequirementType(RequirementType type) {
        return (root, query, cb) -> cb.equal(root.get("requirementType"), type);
    }

    public static Specification<Requirement> hasWave(Integer wave) {
        return (root, query, cb) -> cb.equal(root.get("wave"), wave);
    }

    public static Specification<Requirement> searchTitleOrStatement(String search) {
        return (root, query, cb) -> {
            var pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern), cb.like(cb.lower(root.get("statement")), pattern));
        };
    }

    public static Specification<Requirement> fromFilter(RequirementFilter filter) {
        Specification<Requirement> spec = Specification.where(null);
        if (filter.status() != null) {
            spec = spec.and(hasStatus(filter.status()));
        }
        if (filter.requirementType() != null) {
            spec = spec.and(hasRequirementType(filter.requirementType()));
        }
        if (filter.wave() != null) {
            spec = spec.and(hasWave(filter.wave()));
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            spec = spec.and(searchTitleOrStatement(filter.search()));
        }
        return spec;
    }
}
