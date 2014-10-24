package net.anthavio.vinbudin;

import net.anthavio.vinbudin.OAuthController.OAuthProvider;

/**
 * 
 * @author martin.vanek
 *
 */
public class ChatMan {

	private String name;

	private OAuthProvider from;

	public ChatMan(String name, OAuthProvider from) {
		this.name = name;
		this.from = from;
	}

	public OAuthProvider getFrom() {
		return from;
	}

	public void setFrom(OAuthProvider from) {
		this.from = from;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		ChatMan other = (ChatMan) obj;
		if (from != other.from)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
