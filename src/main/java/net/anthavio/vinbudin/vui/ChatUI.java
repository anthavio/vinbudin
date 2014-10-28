package net.anthavio.vinbudin.vui;

import java.text.SimpleDateFormat;
import java.util.Iterator;

import net.anthavio.aspect.Logged;
import net.anthavio.vaadin.CallbackRegistry;
import net.anthavio.vinbudin.ChatMan;
import net.anthavio.vinbudin.ChatMessage;
import net.anthavio.vinbudin.ChatMessageListener;
import net.anthavio.vinbudin.ChatService;
import net.anthavio.vinbudin.OAuthController.OAuthProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.spring.VaadinUI;

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
public class ChatUI extends UI {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final long serialVersionUID = 1L;

	public static final String ME_KEY = "ME";

	@Autowired
	ChatService service;

	@Autowired
	CallbackRegistry registry;

	MenuBar menubar = new MenuBar();
	MenuItem miLogin;
	LoginCommand loginCommand = new LoginCommand();
	MenuItem miLogout;

	TextField fieldMessage = new TextField();
	Button buttonSend = new Button("Send");
	Button buttonCancel = new Button("Cancel");
	Button buttonClear = new Button("Clear");

	Label labelBoard = new Label("", ContentMode.PREFORMATTED);

	@Logged
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
			ConfirmDialog.show(this, "Please Confirm:", "Delete all messages?", "Yes", "Nope", new ConfirmDialog.Listener() {
				public void onClose(ConfirmDialog dialog) {
					if (dialog.isConfirmed()) {
						service.clearMessages();
						ChatMessage message = new ChatMessage(getMe(), "Cleared discussion...");
						service.addMessage(message);
					}
				}
			});
		});

		menubar.setWidth("100%");
		miLogin = menubar.addItem("Login", null);
		for (OAuthProvider p : OAuthProvider.values()) {
			miLogin.addItem(p.name(), loginCommand);
		}
		miLogout = menubar.addItem("Replace Me", null);
		miLogout.addItem("Logout", (selectedItem) -> {
			//Say good bye...
				ChatMessage message = new ChatMessage(getMe(), "Logged out...");
				service.addMessage(message);

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
		registry.attach(this, new ChatMessageListener() {

			@Override
			public void onEvent(ChatMessage message) {
				onChatMessage(message);
			}
		});
	}

	@Logged
	@Override
	protected void refresh(VaadinRequest request) {
		setUiStateByLogin();
		refreshMessageBoard();
		fieldMessage.focus();
	}

	@Logged
	@Override
	public void detach() {
		registry.detach(this);
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

	@Logged
	//@EventBusListenerMethod
	public void onChatMessage(ChatMessage message) {
		//Push magic happens here
		access(new Runnable() {
			@Override
			public void run() {
				try {
					if (message.getAuthor() != getMe()) {
						Notification n = new Notification("Message from " + message.getAuthor().getName(), message.getText(),
								Type.TRAY_NOTIFICATION);
						n.show(getPage());
					}
					refreshMessageBoard();
				} catch (Exception x) {
					logger.warn("onChatMessage push failed", x);
				}
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
