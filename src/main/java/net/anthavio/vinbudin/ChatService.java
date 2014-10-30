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

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;

import net.anthavio.vaadin.CallbackRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * @author martin.vanek
 *
 */
@Service
public class ChatService {

	private CallbackRegistry registry;

	@Autowired
	public ChatService(CallbackRegistry registry) {
		this.registry = registry;
	}

	private AutoDiscardingDeque<ChatMessage> messages = new AutoDiscardingDeque<ChatMessage>(100);

	public void addMessage(ChatMessage message) {
		messages.add(message);
		registry.publish(message);
	}

	public void clearMessages() {
		messages.clear();
	}

	public Iterator<ChatMessage> messagesIterator() {
		return messages.descendingIterator();
	}

	public static class AutoDiscardingDeque<E> extends LinkedBlockingDeque<E> {

		private static final long serialVersionUID = 1L;

		public AutoDiscardingDeque() {
			super();
		}

		public AutoDiscardingDeque(int capacity) {
			super(capacity);
		}

		@Override
		public synchronized boolean offerFirst(E e) {
			if (remainingCapacity() == 0) {
				removeLast();
			}
			super.offerFirst(e);
			return true;
		}
	}

}
