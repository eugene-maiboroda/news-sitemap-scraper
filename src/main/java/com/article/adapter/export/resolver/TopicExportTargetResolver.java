package com.article.adapter.export.resolver;

import com.article.adapter.export.model.JsonExportTarget;
import com.article.adapter.export.model.PreparedArticleExport;
import com.article.adapter.shared.FileNaming;
import com.article.domain.article.TaxonomyLink;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TopicExportTargetResolver implements ExportTargetResolver {

    @Override
    public List<JsonExportTarget> resolve(PreparedArticleExport article) {
        List<TaxonomyLink> topics = article.metadata().topics();
        Set<JsonExportTarget> targets = new LinkedHashSet<>();
        List<String> topicNames = new ArrayList<>();

        for (int index = 0; index < topics.size(); index++) {
            TaxonomyLink topic = topics.get(index);
            topicNames.add(topic.name());

            boolean standaloneCategory = index == 0
                    && topics.size() == 1
                    && article.metadata().mainCategory() != null;
            if (topic.url() == null || (topic.name() == null && !standaloneCategory)) {
                continue;
            }

            List<String> names = Collections.unmodifiableList(new ArrayList<>(topicNames));
            List<String> hierarchy = FileNaming.buildHierarchy(article.metadata().mainCategory(), names);
            String slug = FileNaming.categorySlug(hierarchy.getLast());
            String fileName = slug + "_" + FileNaming.shortHash(
                    targetKey(topic.url(), index + 1, article.metadata().mainCategory(), names)
            ) + ".json";
            targets.add(JsonExportTarget.topic(
                    Path.of(slug, fileName),
                    topic.url(),
                    hierarchy
            ));
        }
        return List.copyOf(targets);
    }

    private String targetKey(
            String topicUrl,
            int level,
            String mainCategory,
            List<String> topicNames
    ) {
        return topicUrl + "#" + level + "#" + Objects.toString(mainCategory, "") + "#"
                + String.join(">", topicNames.stream()
                .map(name -> Objects.toString(name, ""))
                .toList());
    }
}
