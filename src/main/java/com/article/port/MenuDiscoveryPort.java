package com.article.port;

import java.util.List;

public interface MenuDiscoveryPort {

    List<Section> sections();

    SectionResult discover(Section section);

    record Section(

            String path,

            List<List<String>> hierarchies,

            boolean collectCards
    ) {}

    record Card(

            String articleUrl,

            String title,

            String description
    ) {}

    record SectionResult(

            List<Card> cards,

            String error
    ) {}
}
