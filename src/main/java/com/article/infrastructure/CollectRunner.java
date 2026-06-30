package com.article.infrastructure;

import com.article.application.ArticleCollectorService;
import com.article.domain.TeamConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CollectRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CollectRunner.class);

    private final ArticleCollectorService collectorService;

    public CollectRunner(ArticleCollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @Override
    public void run(String... args) {
        List<TeamConfig> teams = configureTeams();

        if (teams.isEmpty()) {
            log.warn("No teams configured. Add teams to CollectRunner.configureTeams()");
            return;
        }

        collectorService.run(teams);
    }

    private List<TeamConfig> configureTeams() {
        List<TeamConfig> teams = new ArrayList<>();

//        teams.add(new TeamConfig(
//                "Memphis Grizzlies",
//                "https://www.si.com/nba/grizzlies/onsi/sitemap.xml",
//                LocalDate.of(2023, 2, 1),
//                LocalDate.of(2024, 3, 21)
//        ));

//        teams.add(new TeamConfig(
//                "Baltimore Ravens",
//                "https://www.si.com/nfl/ravens/onsi/sitemap.xml",
//                LocalDate.of(2023, 1, 1),
//                LocalDate.of(2024, 3, 21)
//        ));

        return teams;
    }
}
