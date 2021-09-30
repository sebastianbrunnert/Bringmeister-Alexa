package de.sebastianbrunnert.bringmeistervoice.rest;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServlet;

/**
 * Spring Boot Konfiguration, die das Alexa Servlet unter /endpoint/ veröffentlichen soll.
 *
 * @author Sebastian Brunnert
 */
@Configuration
public class WebConfig {

    /**
     * Spezifischer Alexa-Teil
     *
     * @return
     */
    @Bean
    public ServletRegistrationBean<HttpServlet> alexaServlet() {
        ServletRegistrationBean<HttpServlet> servRegBean = new ServletRegistrationBean<>();
        servRegBean.setServlet(new AlexaServlet());
        servRegBean.addUrlMappings("/endpoint/*");
        servRegBean.setLoadOnStartup(1);
        return servRegBean;
    }

}