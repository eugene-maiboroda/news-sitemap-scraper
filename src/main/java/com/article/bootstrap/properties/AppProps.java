package com.article.bootstrap.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "server")
public class AppProps {

    private String host;
    private int port;
    private Servlet servlet = new Servlet();

    @Getter
    @Setter
    public static class Servlet {

        private String contextPath = "";
    }
}
