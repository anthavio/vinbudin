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
