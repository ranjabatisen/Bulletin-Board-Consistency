package com.services;


public interface Service {
	
	
	/**
	 * Read a list of articles and display the list 1 item per line
	 * @param articleId
	 * @return
	 */
	public void read();
	
	/**
	 * Post an article
	 * @param articleContents
	 * @return
	 */
	public void post(String articleContents);
	
	/**TO DO : return article
	 * Choose one of articles and display its contents
	 * @param articleId
	 * @return
	 */
	public void choose(String articleId);
	
	/**
	 * Reply to an existing article (also posts as a new article)
	 * @param articleContents
	 * @param articleID 
	 * @return
	 */
	public void reply(String articleContents, int articleID);
}