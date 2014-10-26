package vinbudin;

import net.anthavio.vinbudin.VinbudinBoot;

import org.springframework.boot.SpringApplication;

/**
 * Test scoped main() can access test classes/resources
 * 
 * @author martin.vanek
 *
 */
public class TestVinbudinBoot {

	public static void main(String[] args) {
		SpringApplication.run(VinbudinBoot.class, args);
	}
}
