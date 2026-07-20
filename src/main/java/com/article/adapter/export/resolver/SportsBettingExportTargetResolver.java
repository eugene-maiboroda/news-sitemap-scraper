package com.article.adapter.export.resolver;

import com.article.adapter.export.model.JsonExportTarget;
import com.article.adapter.export.model.PreparedArticleExport;
import com.article.domain.article.TaxonomyLink;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SportsBettingExportTargetResolver implements ExportTargetResolver {

    private static final String SPORTS_BETTING_TAG = "sports betting";

    @Override
    public List<JsonExportTarget> initialTargets() {
        return java.util.Arrays.stream(SportsBettingExportTarget.values())
                .map(this::toJsonTarget)
                .toList();
    }

    @Override
    public List<JsonExportTarget> resolve(PreparedArticleExport article) {
        Set<String> tagNames = normalize(article.metadata().tags());
        if (!tagNames.contains(SPORTS_BETTING_TAG)) {
            return List.of();
        }

        List<JsonExportTarget> targets = new ArrayList<>();
        for (SportsBettingExportTarget target : SportsBettingExportTarget.values()) {
            if (tagNames.contains(normalize(target.getRequiredTag()))) {
                targets.add(toJsonTarget(target));
            }
        }
        return targets;
    }

    private JsonExportTarget toJsonTarget(SportsBettingExportTarget target) {
        return JsonExportTarget.topic(
                Path.of(target.getFileName()),
                target.getTopicUrl(),
                target.getHierarchy()
        );
    }

    private Set<String> normalize(List<TaxonomyLink> tags) {
        return tags.stream()
                .map(TaxonomyLink::name)
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
