package net.anthavio.vinbudin;

import net.anthavio.aspect.Logged

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.web.SpringBootServletInitializer
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@EnableAutoConfiguration
class Application extends SpringBootServletInitializer {

	@Logged
	@RequestMapping(method = RequestMethod.GET)
	String get() {
		"home"
	}

	static void main(String[] args) {
		SpringApplication.run this, args
	}

	@Override
	SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		application.sources Application
	}
}