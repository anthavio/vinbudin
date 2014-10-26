package net.anthavio.vinbudin.vui;

import java.text.SimpleDateFormat;
import java.util.Iterator;

import net.anthavio.vinbudin.ChatMan;
import net.anthavio.vinbudin.ChatMessage;
import net.anthavio.vinbudin.ChatMessageListener;
import net.anthavio.vinbudin.ChatService;
import net.anthavio.vinbudin.OAuthController.OAuthProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.VaadinUI;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBusListenerMethod;
import org.vaadin.spring.events.EventBusScope;
import org.vaadin.spring.events.EventScope;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * 
 * @author martin.vanek
 *
 */
@VaadinUI
@PreserveOnRefresh
@Theme("valo")
@Push(transport = Transport.WEBSOCKET)
public class ChatUI extends UI implements ChatMessageListener {

	private static final long serialVersionUID = 1L;

	public static final String ME_KEY = "ME";

	@Autowired
	ChatService service;

	@Autowired
	@EventBusScope(EventScope.APPLICATION)
	EventBus busAppScope;

	MenuBar menubar = new MenuBar();
	MenuItem miLogin;
	LoginCommand loginCommand = new LoginCommand();
	MenuItem miLogout;

	TextField fieldMessage = new TextField();
	Button buttonSend = new Button("Send");
	Button buttonCancel = new Button("Cancel");
	Button buttonClear = new Button("Clear");

	Label labelBoard = new Label("", ContentMode.PREFORMATTED);

	@Override
	protected void init(VaadinRequest request) {

		fieldMessage.setMaxLength(100);
		fieldMessage.setEnabled(false);
		fieldMessage.setWidth("100%");
		fieldMessage.addTextChangeListener(event -> {
			String text = event.getText();
			buttonSend.setEnabled(text != null && text.length() > 2);
		});

		buttonSend.setEnabled(false);
		buttonSend.setClickShortcut(KeyCode.ENTER);
		buttonSend.addStyleName(ValoTheme.BUTTON_PRIMARY);

		buttonSend.addClickListener(event -> {
			ChatMessage message = new ChatMessage(getMe(), fieldMessage.getValue());
			service.addMessage(message);
			busAppScope.publish(this, message);
			fieldMessage.setValue("");
			buttonSend.setEnabled(false);
		});

		buttonCancel.setClickShortcut(ShortcutAction.KeyCode.ESCAPE);
		buttonCancel.addClickListener(event -> {
			fieldMessage.setValue("");
			buttonSend.setEnabled(false);
		});

		buttonClear.setEnabled(false);
		buttonClear.addClickListener(event -> {
			service.clearMessages();
			ChatMessage message = new ChatMessage(getMe(), "Clearing discussion now...");
			service.addMessage(message);
			busAppScope.publish(this, message);
		});

		menubar.setWidth("100%");
		miLogin = menubar.addItem("Login", null);
		for (OAuthProvider p : OAuthProvider.values()) {
			miLogin.addItem(p.name(), loginCommand);
		}
		miLogout = menubar.addItem("Replaceme", null);
		miLogout.addItem("Logout", (selectedItem) -> {
			setMe(null);
			setUiStateByLogin();
		});

		HorizontalLayout lSending = new HorizontalLayout(fieldMessage, buttonSend, buttonCancel, buttonClear);
		lSending.setWidth("100%");
		lSending.setSpacing(true);
		lSending.setExpandRatio(fieldMessage, 1);

		labelBoard.setSizeFull();

		VerticalLayout layout = new VerticalLayout(menubar, lSending, labelBoard);
		layout.setSpacing(true);
		layout.setMargin(true);
		setContent(layout);
		setSizeFull();

		setUiStateByLogin();
		refreshMessageBoard();
		fieldMessage.focus();
		//service.register(this);
		busAppScope.subscribe(this);
	}

	@Override
	protected void refresh(VaadinRequest request) {
		setUiStateByLogin();
		refreshMessageBoard();
		fieldMessage.focus();
	}

	@Override
	public void detach() {
		//service.unregister(this);
		busAppScope.unsubscribe(this);
		super.detach();
	}

	private ChatMan getMe() {
		return (ChatMan) UI.getCurrent().getSession().getSession().getAttribute(ME_KEY);
	}

	private void setMe(ChatMan me) {
		UI.getCurrent().getSession().getSession().setAttribute(ME_KEY, me);
	}

	private void setUiStateByLogin() {
		boolean loggedin = getMe() != null;
		fieldMessage.setEnabled(loggedin);
		buttonClear.setEnabled(loggedin);

		miLogin.setVisible(!loggedin);
		miLogout.setVisible(loggedin);
		if (loggedin) {
			miLogout.setText(getMe().getName());
		}
	}

	@EventBusListenerMethod
	@Override
	public void onChatMessage(ChatMessage message) {
		//Push magic happens here
		access(new Runnable() {
			@Override
			public void run() {
				if (message.getAuthor() != getMe()) {
					Notification n = new Notification("Message from " + message.getAuthor().getName(), message.getText(),
							Type.TRAY_NOTIFICATION);
					n.show(getPage());
				}
				refreshMessageBoard();
			}
		});

	}

	private void refreshMessageBoard() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm z");
		Iterator<ChatMessage> messages = service.messagesIterator();
		StringBuilder sb = new StringBuilder();
		messages.forEachRemaining(message -> {
			sb.append(sdf.format(message.getDate())).append(", ").append(message.getAuthor().getName()).append(" [")
					.append(message.getAuthor().getFrom().name()).append("]: ").append(message.getText()).append("\n");
		});
		labelBoard.setValue(sb.toString());
	}

	private class LoginCommand implements Command {

		private static final long serialVersionUID = 1L;

		@Override
		public void menuSelected(MenuItem selectedItem) {
			OAuthProvider provider = OAuthProvider.getByName(selectedItem.getText());
			String url = provider.getOAuth().getAuthorizationUrl(provider.getScopes(),
					String.valueOf(System.currentTimeMillis()));
			getUI().getPage().setLocation(url); //redirect to OAuth...
		}
	}

}
