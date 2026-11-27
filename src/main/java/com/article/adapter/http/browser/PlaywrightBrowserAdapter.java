package com.article.adapter.http.browser;

import com.article.application.PipelineRunSettings;
import com.article.bootstrap.properties.BrowserProps;
import com.article.port.out.BrowserPort;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaywrightBrowserAdapter implements BrowserPort {

    private final PipelineRunSettings runSettings;

    @Override
    public Optional<byte[]> interceptResponseBody(
            String pageUrl,
            String responseUrlContains,
            List<String> consentClickSelectors,
            String scrollIntoViewSelector) {

        BrowserProps browserProps = runSettings.current().browser();
        if (!browserProps.enabled()) {
            return Optional.empty();
        }

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(browserProps.headless());
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(options);
             BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.setDefaultTimeout(browserProps.timeoutMs());

            Response response = page.waitForResponse(resp -> resp.url().contains(responseUrlContains), () -> {
                        page.navigate(pageUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                        acceptCookieConsent(page, consentClickSelectors);
                        scrollIntoView(page, scrollIntoViewSelector);
                    });

            return Optional.ofNullable(response.body());
        } catch (Exception e) {
            log.debug("[BROWSER] No response matching '{}' on {}: {}", responseUrlContains, pageUrl, e.getMessage());
            return Optional.empty();
        }
    }

    private void acceptCookieConsent(Page page, List<String> selectors) {
        for (String selector : selectors) {
            try {
                Locator button = page.locator(selector).first();
                if (button.isVisible()) {
                    button.click();
                    return;
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void scrollIntoView(Page page, String selector) {
        try {
            page.locator(selector).first().scrollIntoViewIfNeeded();
        } catch (Exception e) {
            log.debug("[BROWSER] Could not scroll {} into view: {}", selector, e.getMessage());
        }
    }
}
