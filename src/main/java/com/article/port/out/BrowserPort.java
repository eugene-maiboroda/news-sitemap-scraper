package com.article.port.out;

import java.util.List;
import java.util.Optional;

public interface BrowserPort {

    Optional<byte[]> interceptResponseBody(
            String pageUrl,
            String responseUrlContains,
            List<String> consentClickSelectors,
            String scrollIntoViewSelector);
}
