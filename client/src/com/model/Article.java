package com.model;



import java.util.List;
import java.util.ArrayList;

public class Article {

	public int id;
	public String contents = null;
	public Integer parentId = -1;
	public List<Article> replies = new ArrayList<Article>();

	public int version;
        
        public Article() {
            
        }

	public Article(int id, int parentId, String content, int version, List<Article> replies) {
		this.id = id;
		this.contents = content;
		this.parentId = parentId;
		this.version = version;
		this.replies = replies;
	}
	
	public Article(int id, String contents) {
		this.id = id;
		this.contents = contents;
		this.version = 1;
	}

	public Integer getVersion() {
		return this.version;
	}

	public Integer getId() {
		return id;
	}

	public void updateVersion() {
		this.version = this.version + 1;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.updateVersion();
		this.contents = contents;
	}

	public List<Article> getReplies() {
		return replies;
	}

	public void setReplies(List<Article> replies) {
		this.updateVersion();
		this.replies = replies;
	}

	public void addReply(Article reply) {
		this.updateVersion();
		this.replies.add(reply);
	}
	
	public Integer getParentID() {
		return parentId;
	}

	public void setParentID(Integer parentID) {
		this.parentId = parentID;
	}


	public String toString() {
		String str = "";
		if (this.contents == null) {
			return null;
		}
		str = str + this.id + this.contents;
		String tabStr = "\t";
		for (Article reply : this.replies) {
			str = str + "\n";
			str = str + tabStr;
			str = str + reply.toString();
		}

		return str;

	}

	
}