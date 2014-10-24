package net.anthavio.vinbudin;

import java.util.Date;

/**
 * 
 * @author martin.vanek
 *
 */
public class ChatMessage {

	private final ChatMan author;

	private final String text;

	private final Date date;

	public ChatMessage(ChatMan who, String text) {
		this.author = who;
		this.text = text;
		this.date = new Date();
	}

	public String getText() {
		return text;
	}

	public ChatMan getAuthor() {
		return author;
	}

	public Date getDate() {
		return date;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + ((author == null) ? 0 : author.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChatMessage other = (ChatMessage) obj;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		if (author == null) {
			if (other.author != null)
				return false;
		} else if (!author.equals(other.author))
			return false;
		return true;
	}

}
