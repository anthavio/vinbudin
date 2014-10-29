package net.anthavio.vinbudin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSessionListener;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.vaadin.spring.config.VaadinConfiguration;
import org.vaadin.spring.servlet.SpringAwareVaadinServlet;

@Configuration
@ComponentScan
//@EnableVaadin
@EnableAutoConfiguration
@Import(VaadinConfiguration.class)
public class VinbudinBoot extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(VinbudinBoot.class/*, MvcSpringConfig.class*/);
	}

	@Bean
	public ServletRegistrationBean SpringAwareVaadinServlet() {
		return new ServletRegistrationBean(new SpringAwareVaadinServlet(), "/ui/*", "/VAADIN/*");
	}

	@Bean
	public HttpSessionListener AtmosphereSessionSupport() {
		return new org.atmosphere.cpr.SessionSupport();
	}

	@Bean
	public net.anthavio.vaadin.CallbackRegistry CallbackRegistry() {
		ExecutorService executor = Executors.newFixedThreadPool(10);
		return new net.anthavio.vaadin.CallbackRegistry(executor, 3, TimeUnit.SECONDS);
	}

	/*
		@Bean
		public ServletRegistrationBean StaticContentServlet() {
			//return new ServletRegistrationBean(new SpringAwareTouchKitServlet(), "/vui/*");
			return new ServletRegistrationBean(new StaticContentServlet(), "/VAADIN/*");
		}
	*/

	public static void main(String[] args) {
		SLF4JBridgeHandler.install();
		SpringApplication.run(VinbudinBoot.class, args);
	}

}