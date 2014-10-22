package net.anthavio.vinbudin;

import net.anthavio.aspect.Logged;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.vaadin.spring.EnableVaadin;

@ComponentScan
@EnableVaadin
@EnableAutoConfiguration
public class VinbudinBoot extends SpringBootServletInitializer {

	@Logged
	@RequestMapping(method = RequestMethod.GET)
	public String get() {
		System.out.println("ApplicationJxx");
		return "home Jxxxx";
	}

	@Bean
	public ChatService service() {
		return new ChatService();
	}

	public static void main(String[] args) {
		System.out.println("ApplicationJxxxxxx");
		SpringApplication.run(VinbudinBoot.class, args);
	}

	/*
		@Bean
		@Scope("prototype")
		public UI ui() {
			return new MyVaadinUI();
		}

		@Bean
		public ServletRegistrationBean servletRegistrationBean() {
			System.out.println("xxxxxxxxxxxx");
			final ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(
					new ru.xpoft.vaadin.SpringVaadinServlet(), "/*", "/VAADIN/*");
			return servletRegistrationBean;
		}

		@Override
		public SpringApplicationBuilder configure(SpringApplicationBuilder application) {
			System.out.println("zzzzzzzzzzzzzz");
			return application.sources(ApplicationJ.class);
		}
	*/
}