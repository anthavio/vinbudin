package net.anthavio.vinbudin.vui;

import java.util.Iterator;

import net.anthavio.vinbudin.ChatMessageListener;
import net.anthavio.vinbudin.ChatService;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.VaadinUI;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@Push
@VaadinUI
@Theme("valo")
public class ChatUI extends UI implements ChatMessageListener {

	private static final long serialVersionUID = 1L;

	@Autowired
	ChatService service;

	TextField fieldMessage = new TextField();
	Button buttonSend = new Button("Send");
	Button buttonCancel = new Button("Cancel");
	Button buttonClear = new Button("Clear");
	Label labelBoard = new Label("", ContentMode.PREFORMATTED);

	@Override
	protected void init(VaadinRequest request) {

		labelBoard.setSizeFull();
		buttonSend.setEnabled(false);

		buttonSend.setClickShortcut(KeyCode.ENTER);
		buttonSend.addStyleName(ValoTheme.BUTTON_PRIMARY);

		buttonCancel.setClickShortcut(ShortcutAction.KeyCode.ESCAPE);

		//fieldMessage.setNullRepresentation("Say something...");
		//fieldMessage.setNullSettingAllowed(false);
		fieldMessage.setWidth("100%");
		fieldMessage.addTextChangeListener(event -> {
			String text = event.getText();
			buttonSend.setEnabled(text != null && text.length() > 2);
		});

		buttonSend.addClickListener(event -> {
			service.addMessage("Anonymous", fieldMessage.getValue());
			fieldMessage.setValue("");
			buttonSend.setEnabled(false);
			//updateBoard();
			});

		buttonCancel.addClickListener(event -> {
			fieldMessage.setValue("");
			buttonSend.setEnabled(false);
		});

		buttonClear.addClickListener(event -> {
			service.clearMessages();
		});

		HorizontalLayout lSending = new HorizontalLayout(fieldMessage, buttonSend, buttonCancel);
		lSending.setWidth("95%");
		lSending.setSpacing(true);
		lSending.setExpandRatio(fieldMessage, 100);

		VerticalLayout layout = new VerticalLayout(lSending, labelBoard);
		//layout.setSizeFull();
		setContent(layout);
		setSizeFull();

		updateBoard();
		fieldMessage.focus();
		service.register(this);
	}

	@Override
	public void detach() {
		service.unregister(this);
		super.detach();
	}

	@Override
	public void onChatMessage(String message) {
		access(new Runnable() {
			@Override
			public void run() {
				Notification n = new Notification("Message received", message, Type.TRAY_NOTIFICATION);
				n.show(getPage());
				updateBoard();
			}
		});

	}

	private void updateBoard() {
		Iterator<String> messages = service.messagesIterator();
		StringBuilder sb = new StringBuilder();
		messages.forEachRemaining(message -> {
			sb.append(message).append("\n");
		});
		labelBoard.setValue(sb.toString());
	}

}
