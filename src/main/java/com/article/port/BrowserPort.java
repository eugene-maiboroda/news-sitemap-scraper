package com.article.port;

import java.util.List;
import java.util.Optional;

public interface BrowserPort {

    /**
     * Opens {@code pageUrl}, clicks the first visible selector from {@code consentClickSelectors}
     * (if any), scrolls {@code scrollIntoViewSelector} into view, then waits for a network response
     * whose URL contains {@code responseUrlContains} and returns its body. The page is closed before
     * returning. Returns {@link Optional#empty()} if no matching response arrives before timeout —
     * an expected outcome, not an error.
     */
    Optional<byte[]> interceptResponseBody(
            String pageUrl,
            String responseUrlContains,
            List<String> consentClickSelectors,
            String scrollIntoViewSelector);
}
