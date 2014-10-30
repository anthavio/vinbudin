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
