package com.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

// Making Article class Serializable to pass byte array across TCP
public class Article {
	@JsonProperty("id")
	private Integer id;
	@JsonProperty("contents")
	private String contents = null;
	@JsonProperty("replies")
	private List<Article> replies = new ArrayList<Article>();
	@JsonProperty("version")
	private int version;
	@JsonProperty("parentId")
	private int parentId = 0;
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
	@JsonProperty("version")
	public int getVersion() {
		return this.version;
	}
	@JsonProperty("version")
	public void setVersion( int version) {
		this.version = version;
	}
	@JsonProperty("id")
	public Integer getId() {
		return id;
	}

	public void updateVersion() {
		this.version = this.version + 1;
	}
	@JsonProperty("id")
	public void setId(Integer id) {
		this.id = id;
	}
	@JsonProperty("contents")
	public String getContents() {
		return contents;
	}
	@JsonProperty("contents")
	public void setContents(String contents) {
		//this.updateVersion();
		this.contents = contents;
	}
	@JsonProperty("replies")
	public List<Article> getReplies() {
		return replies;
	}
	@JsonProperty("replies")
	public void setReplies(List<Article> replies) {
		//this.updateVersion();
		this.replies = replies;
	}

	public void addReply(Article reply) {
		boolean add = true;
		if(this.getReplies().size()>0) {
			for(Article currReply: this.getReplies()) {
				if(currReply.getId() == reply.getId() && currReply.getVersion() == reply.getVersion()) {
					add = add & false;
				}else {
					add = true;
				}
				
			}
		}else {
			add = true;
		}
		if(add) {
			this.updateVersion();
			this.replies.add(reply);
		}
		
	}
	
	@Override
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
	@JsonProperty("parentId")
	public int getParentId() {
		return parentId;
	}
	@JsonProperty("parentId")
	public void setParentId(int parentId) {
		this.parentId = parentId;
	}

}