package net.anthavio.vinbudin;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;

import org.springframework.stereotype.Component;

@Component
public class ChatService {

	private final List<ChatMessageListener> listeners = new CopyOnWriteArrayList<ChatMessageListener>();

	private AutoDiscardingDeque<String> messages = new AutoDiscardingDeque<String>(100);

	public void register(ChatMessageListener listener) {
		listeners.add(listener);
	}

	public void unregister(ChatMessageListener listener) {
		listeners.remove(listener);
	}

	public void addMessage(String author, String message) {
		messages.add(author + " : " + message);

		listeners.forEach(listener -> {
			try {
				listener.onChatMessage(message);
			} catch (Exception x) {
				x.printStackTrace();
			}
		});

	}

	public void clearMessages() {
		messages.clear();
	}

	public Iterator<String> messagesIterator() {
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
