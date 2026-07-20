package com.article.infrastructure;

import com.article.bootstrap.properties.BrowserProps;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@ConditionalOnProperty(prefix = "browser", name = "enabled", havingValue = "true")
public class PlaywrightConfig {

    @Bean(destroyMethod = "close")
    @Lazy
    public Playwright playwright() {
        return Playwright.create();
    }

    @Bean(destroyMethod = "close")
    @Lazy
    public Browser browser(Playwright playwright, BrowserProps props) {
        return playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(props.headless()));
    }
}
