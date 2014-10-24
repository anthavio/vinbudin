package net.anthavio.vinbudin;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final List<ChatMessageListener> listeners = new CopyOnWriteArrayList<ChatMessageListener>();

	private AutoDiscardingDeque<ChatMessage> messages = new AutoDiscardingDeque<ChatMessage>(100);

	public void register(ChatMessageListener listener) {
		listeners.add(listener);
	}

	public void unregister(ChatMessageListener listener) {
		listeners.remove(listener);
	}

	public void addMessage(ChatMessage message) {
		messages.add(message);
		/*
		listeners.forEach(listener -> {
			try {
				listener.onChatMessage(message);
			} catch (Exception x) {
				logger.warn("Listener " + listener + " failed");
			}
		});
		*/
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
