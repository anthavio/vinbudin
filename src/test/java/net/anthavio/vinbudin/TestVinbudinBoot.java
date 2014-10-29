package net.anthavio.vinbudin;

import org.springframework.boot.SpringApplication;

/**
 * Test scoped main() can access test classes/resources (logback-test.xml specifically)
 * 
 * @author martin.vanek
 *
 */
public class TestVinbudinBoot {

	public static void main(String[] args) {
		SpringApplication.run(VinbudinBoot.class, args);
	}
}
