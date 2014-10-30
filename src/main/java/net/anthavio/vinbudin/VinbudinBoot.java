/**
 * This file is part of vinbudin.
 *
 * vinbudin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * vinbudin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with vinbudin.  If not, see <http://www.gnu.org/licenses/>.
 */
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