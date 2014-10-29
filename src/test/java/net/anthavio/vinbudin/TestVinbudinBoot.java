package net.anthavio.vinbudin;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.SpringApplication;

/**
 * Test scoped main() can access test classes/resources (logback-test.xml specifically)
 * 
 * @author martin.vanek
 *
 */
public class TestVinbudinBoot {

	public static void main(String[] args) {
		SLF4JBridgeHandler.install();
		SpringApplication.run(VinbudinBoot.class, args);
	}
}
