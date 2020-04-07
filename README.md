# Bulletin-Board-Consistency
(By Ranja Bati Sen (ranjabatisen), Mayura Nene (nenemayura) and Roopana Vuppalapati Chenchu (Roopana))

Objective : 
To design a system where multiple clients can post articles, read a list of articles, choose an article to see its content and reply to an existing article. There will be multiple servers with a coordinator server. 3 kinds of consistencies will be implemented : Sequential consistency, Quorum consistency and Read-your-Write consistency.

Design:
Main components involved: 
1. Client

      Client UI (class Name: ClientUIConnectorServlet)
      
      Client (class Name: ClientExecutor)
2. Server 

      Coordinator Server (class Name: CoordinatorServer)
      
      Server (class Name: ServerExecutor)
      
Protocols used : TCP and UDP
Technologies used : Java Servlet

Overview of project:
There will be multiple clients and servers (one of the servers is designated as the primary or coordinator). The client does not know or care who the coordinator is. All of the other servers are configured to know who the coordinator is.
The client can perform the following operations :
1. Post an article
2. Read a list of articles and display the list one item per line (maybe just the first few words or the title of
the article)
3. Choose one of articles and display its contents
4. Reply to an existing article (also posts as a new article)

Each article has an internal unique ID that is generated by the coordinator. So the contacted server (on a post or reply) will ask the coordinator for the next unique ID to use. The server will order the articles in increasing order using the ID (1, 2 …) as this captures time order. For the Read command, the client should print the articles in this order using indentation for replies :
1. Article 1
2. Article 2
      3. Reply to article 2
             4. Reply to article 3
5. Article 5
(The read operation prints 5 lines per page.)

The 3 types of consistencies implemented :
1. Sequential consistency : primary backup protocol used.
2. Quorum consistency : Given N replicas, there will be a read quorum (NR) which is an arbitrary collection of servers in order to read/choose, and a write quorum (NW), an arbitrary collection of servers in order to post/reply for the client. The values of NR and NW are subject to the following two constraints:
    1. NR + NW > N
    2. NW > N/2
3. Read your write consistency : local-write protocol used.
