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
package net.anthavio.vaadin;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.anthavio.vinbudin.ChatMan;
import net.anthavio.vinbudin.ChatMessage;
import net.anthavio.vinbudin.ChatMessageListener;
import net.anthavio.vinbudin.vui.ChatUI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.UI;

/**
 * 
 * @author martin.vanek
 *
 */
@SuppressWarnings("rawtypes")
public class CallbackRegistry {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Map<UI, CopyOnWriteArrayList<CallbackListener>> uiRegistry = new ConcurrentHashMap<UI, CopyOnWriteArrayList<CallbackListener>>();

	private final Map<Class, CopyOnWriteArrayList<CallbackListener>> eventRegistry = new ConcurrentHashMap<Class, CopyOnWriteArrayList<CallbackListener>>();

	private final ScheduledExecutorService canceller = Executors.newSingleThreadScheduledExecutor();

	private final ExecutorService executor;

	private final int timeout;

	private final TimeUnit unit;

	public CallbackRegistry(ExecutorService executor, int timeout, TimeUnit unit) {
		if (executor == null) {
			throw new IllegalArgumentException("Null ExecutorService");
		}
		this.executor = executor;
		this.timeout = timeout;
		this.unit = unit;
	}

	public void publish(Object event) {
		List<CallbackListener> list = eventRegistry.get(event.getClass());
		if (list == null) {
			logger.warn("No listener registered for " + event.getClass());
			return;
		}

		for (CallbackListener listener : list) {
			if (logger.isDebugEnabled()) {
				logger.debug("Submitting event: " + event + " to: " + listener);
			}
			final CancelFutureHolder holder = new CancelFutureHolder();
			ScheduledFuture<?> cancelFuture = null;
			Future<?> listenerFuture = executor.submit(new Runnable() {

				@Override
				public void run() {
					//unsafe typing but we have registration by event class
					listener.onEvent(event);
					if (holder.cancelFuture != null) {
						holder.cancelFuture.cancel(true);
					}
				}
			});
			//schedule task to cancel original task if it takes too long...
			cancelFuture = canceller.schedule(new Runnable() {
				public void run() {
					boolean cancelled = listenerFuture.cancel(true);
					if (cancelled) {
						logger.info("Cancelled " + listener + ". Timeout " + timeout);
					}
				}
			}, timeout, unit);
			holder.cancelFuture = cancelFuture;

			//} catch (RejectedExecutionException rex) {
		}
	}

	static class CancelFutureHolder {
		ScheduledFuture<?> cancelFuture;
	}

	public <L extends CallbackListener> void attach(UI ui, L listener) {
		logger.info("attach UI: " + ui + " Listener: " + listener);
		Class eventClass = getEventClass(listener);
		CopyOnWriteArrayList<CallbackListener> uiList = uiRegistry.get(ui);
		if (uiList == null) {
			uiList = new CopyOnWriteArrayList<CallbackListener>();
			uiRegistry.put(ui, uiList);
		}
		uiList.add(listener);

		CopyOnWriteArrayList<CallbackListener> eventList = eventRegistry.get(eventClass);
		if (eventList == null) {
			eventList = new CopyOnWriteArrayList<CallbackListener>();
			eventRegistry.put(eventClass, eventList);
		}
		eventList.add(listener);
	}

	public void detach(UI ui) {
		logger.info("detach UI: " + ui);
		//remove all callback for this UI...
		CopyOnWriteArrayList<CallbackListener> callbacks = uiRegistry.remove(ui);
		callbacks.forEach(callback -> {
			Class eventClass = getEventClass(callback);
			CopyOnWriteArrayList<CallbackListener> list = eventRegistry.get(eventClass);
			list.remove(callback);
		});
		logger.debug("detached " + callbacks.size() + " listeners of UI: " + ui);
	}

	/**
	 * listener implementation class must implements some interface that extends CallbackListener, which is parametrized with event type
	 */
	private static Class getEventClass(CallbackListener listener) {
		Class<?>[] interfaces = listener.getClass().getInterfaces();
		//multiple interfaces may be implemented so find that extends CallbackListener...
		for (Class<?> itf : interfaces) {
			if (isExtending(itf, CallbackListener.class)) {
				Type[] ginterfaces = itf.getGenericInterfaces();
				for (Type gitf : ginterfaces) {
					//System.out.println((ParameterizedType) type + " " + (type instanceof ParameterizedType));
					if (gitf instanceof ParameterizedType) {
						//System.out.println(((ParameterizedType) type).getRawType());
						return (Class) ((ParameterizedType) gitf).getActualTypeArguments()[0];
					}
				}
			}
		}
		throw new IllegalArgumentException("Listener " + listener.getClass() + " does not extend CallbackListener<X> ?!?");
	}

	private static boolean isExtending(Class who, Class what) {
		for (Class i : who.getInterfaces()) {
			if (i == what) {
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args) {
		CallbackRegistry registry = new CallbackRegistry(Executors.newFixedThreadPool(1), 3, TimeUnit.SECONDS);
		ChatUI chatUI = new ChatUI();
		registry.attach(chatUI, new ChatMessageListener() {

			@Override
			public void onEvent(ChatMessage event) {
				System.out.println("Blah! " + event);
			}
		});
		registry.publish(new ChatMessage(new ChatMan("name", null), "Reknete prdel!"));

	}
}
