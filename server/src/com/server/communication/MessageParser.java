package com.server.communication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.model.Article;
import com.model.MessagesFromServer;

public class MessageParser {

	// POST;articleId;nR;nW;articlecontents;;version;serverIpPort;clientIpPort
	// REPLY;articleId;nR;nW;articlecontents;articleIDtoreplyTo;version;serverIpPort;clientIpPort
	public static int getArticleID(String message) {
		try {
			return Integer.valueOf(message.split(";")[1]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static String addArticleIdToMessage(String received, int articleID) {
		if(received.contains(";articleid;")) {
			return received.replace(";articleid;", ";" + articleID + ";");
		}
		return received;
		
	}

	public static int getReadQuorum(String received) {
		try {
			return Integer.valueOf(received.split(";")[2]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;

	}

	public static int getWriteQuorum(String received) {
		try {
			return Integer.valueOf(received.split(";")[3]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static String getContents(String received) {
		return received.split(";")[4];
	}

	public static int getParentArticleId(String received) {
		try {
			return Integer.valueOf(received.split(";")[5]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static int getArticleVersion(String received) {
		try {
			return Integer.valueOf(received.split(";")[6]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static String getAppendArticle(String received, String content, int version) {
		String[] msgArray = received.split(";");
		msgArray[4] = content;
		msgArray[6] = String.valueOf(version);
		String messageAppended = "";
		for (int i = 0; i < msgArray.length; i++) {
			if (i == 0) {
				messageAppended = messageAppended + msgArray[i];
			} else {
				messageAppended = messageAppended + ";" + msgArray[i];
			}

		}
		return messageAppended;
	}

	public static String getServerIpPort(String message) {
		return message.split(";")[7];
	}

	public static String getClientIpPort(String message) {
		return message.split(";")[8];
	}

	public static String trimComplete(String message) {
		if (message.startsWith("COMPLETE")) {
			return message.substring(message.indexOf(';') + 1);
		} else {
			return message;
		}
	}

	public static String trimInvalid(String message) {
		if (message.startsWith(MessagesFromServer.INVALID_QUORUM)) {
			return message.substring(message.indexOf(';') + 1);
		} else {
			return message;
		}
	}

	public static String trimPrefix(String message) {
		return message.substring(message.indexOf(';') + 1);
	}

	// POST;articleId;nR;nW;articlecontents;;version;serverIpPort;clientIpPort
	public static String getArticleResponseKey(String received) {
		String[] msgArray = received.split(";");
		// removing version and article contents to construct the key
		return msgArray[0] + ";" + msgArray[1] + ";" + msgArray[2] + ";" + msgArray[3] + ";" + msgArray[5] + ";"
				+ msgArray[7] + ";" + msgArray[8];

	}

	public static String getCompleteMessage(String received) {

		return "COMPLETE;" + received;
	}
	

	public static String serializeListOfArticles(List<Article> recentArticles) {
		
		// limit content of articles to 5
		StringBuffer serializedArticle = new StringBuffer();
		
		for(Article article : recentArticles) {
			
			serializedArticle = serializedArticle.append(article.toString());
		}
		
		return serializedArticle.toString();
	}

	public static List<Article> getTopArticles(ConcurrentHashMap<Integer, Article> articleMap, int readLength) {
		
			List<Article> recentArticles = new ArrayList<Article>();
			List<Map.Entry<Integer, Article>> list = new LinkedList<Map.Entry<Integer, Article>>(articleMap.entrySet());
			int count = 0;
			Collections.sort(list, new Comparator<Map.Entry<Integer, Article>>() {

				@Override
				public int compare(Entry<Integer, Article> o1, Entry<Integer, Article> o2) {
					return (o1.getValue().getId()).compareTo(o2.getValue().getId());
				}
			});
			
	        for (Map.Entry<Integer,Article> article : list) { 
	        	
	        	// check if the article is the parent or reply
	        	
	        	Article readArticle = article.getValue();
    			System.out.println("Read article "+readArticle.toString());

	        	if(count < readLength) {
	        		
	        		if(!readArticle.getReplies().isEmpty() ) {
	        			//recentArticles.add(readArticle);
	        			
	        			List<Article> replies = readArticle.getReplies();
	        			List<Article> includedReplies = new ArrayList();
	        			for(Article reply : replies) {
	        				
	        				if(count >= readLength-1) {
	        					break;
	        				}
	        				includedReplies.add(reply);
	        				count++;
	        			}
	        			Article modifiedArticle = new Article(readArticle.getId(), readArticle.getParentId(), readArticle.getContents(), readArticle.getVersion(), includedReplies);
	        			System.out.println("Modified article "+modifiedArticle.toString());
	        			recentArticles.add(modifiedArticle);
	        			count++;
	        		} else {
	        			recentArticles.add(readArticle);
		            	
		            	count++;
	        		}
	        		
	        		
	        	} else {
	        		break;
	        	}
	        } 
			return recentArticles;
		}

	public static String setContents(String received, String articleMapStr) {
		String[] msgArray = received.split(";");
		msgArray[4] = articleMapStr;
		String messageAppended = "";
		
		for (int i = 0; i < msgArray.length; i++) {
			if (i == 0) {
				messageAppended = messageAppended + msgArray[i];
			} else {
				messageAppended = messageAppended + ";" + msgArray[i];
			}

		}
		return messageAppended;
	}
	public static String serializeServersSet(Set<String> serverIPSet) {
		StringBuffer sb = new StringBuffer();
		
		Iterator<String> itr = serverIPSet.iterator();
		while(itr.hasNext()) {
			
			sb.append(itr.next());
			sb.append("+");
		}
		sb = sb.deleteCharAt(sb.lastIndexOf("+"));
		System.out.println("serialized servers list is "+sb.toString());
		return sb.toString();
	}
		
	public static String trimLocalHostFromServerIpPort(String ip) {
		if(ip.contains("/")) {
			ip = ip.substring(ip.indexOf("/"));
		}
		return ip;
	}	
		

}