package net.anthavio.vaadin;

/**
 * 
 * @author martin.vanek
 *
 */
public interface CallbackListener<E> {

	public void onEvent(E event);
}
