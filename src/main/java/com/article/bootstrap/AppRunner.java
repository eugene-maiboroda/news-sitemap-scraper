package com.article.bootstrap;

import com.article.bootstrap.properties.AppProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
public class AppRunner implements ApplicationRunner {

    private static final String URL_TEMPLATE = "http://%s:%d%s/swagger-ui/index.html";

    private final AppProps appProps;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Swagger UI available at {}", String.format(
                URL_TEMPLATE,
                appProps.getHost(),
                appProps.getPort(),
                appProps.getServlet().getContextPath()
        ));
    }
}
