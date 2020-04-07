package com.ui.communication;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.model.Article;
import java.util.ArrayList;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

/**
 * Servlet implementation class ClientUIConnectorServlet
 */
@WebServlet("/clientuiconnectorservlet")
public class ClientUIConnectorServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	
	/**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
	
	/**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    public static ArrayList<Integer> art_id = new ArrayList<Integer>();
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        DatagramSocket ds = new DatagramSocket(); 
        InetAddress ip = InetAddress.getLocalHost(); 
        byte buf[] = null;
        String inp = "";
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Bulletin Board</title>");            
            out.println("</head>");
            String str = "\"background-image: url('source1.gif');background-repeat: no-repeat;background-attachment: fixed;background-size: 100% 100%;font-weight: bold;color: white;\"";
            out.println("<body style=" + str + ">");
            out.println("Chosen consistency =" + request.getParameter("consistency") + "<br>");
            inp = inp + request.getParameter("consistency");
            out.println("Chosen option = " +request.getParameter("option")+ "<br>");
            String option = request.getParameter("option");
            inp = inp + ",";
            inp = inp + option;
            if(request.getParameter("article_content").length() != 0 && option.equals("post")) {
                out.println("Article content = " +request.getParameter("article_content") + "<br>");
                inp = inp + ",";
                inp = inp + request.getParameter("article_content");
            }
            if(request.getParameter("choose_article_id").length() != 0 && option.equals("choose")) {
                out.println("Article id for choose = " +request.getParameter("choose_article_id") + "<br>");
                inp = inp + ",";
                inp = inp + request.getParameter("choose_article_id");
            }
            if(request.getParameter("reply_article_id").length() != 0 && option.equals("reply") && request.getParameter("reply_article_content").length() != 0) {
                out.println("Article id for reply = " +request.getParameter("reply_article_id") + "<br>");
                out.println("Article content for reply = " +request.getParameter("reply_article_content") + "<br>");
                inp = inp + ",";
                inp = inp + request.getParameter("reply_article_id");
                inp = inp + ",";
                inp = inp + request.getParameter("reply_article_content");
            }
            int port = Integer.parseInt(request.getParameter("port"));
            buf = inp.getBytes(); 
            DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, port);
            ds.send(DpSend);
            byte[] receive = new byte[2048]; 
            DatagramPacket DpReceive = null;
            //DatagramSocket ds1 = new DatagramSocket(1237);
            DpReceive = new DatagramPacket(receive, receive.length);
            ds.receive(DpReceive);
            String receivedStr = data(receive).toString();
            if(option.equals("read")) {
            String[] arr1 = receivedStr.split("Total time taken for this operation ");
            receivedStr = arr1[0];
            if(arr1.length > 1) {
                out.println("Time taken = " + arr1[1] + "<br><br>");
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ConcurrentHashMap<Integer, Article> received = objectMapper.readValue(receivedStr, TypeFactory.defaultInstance().constructMapType(ConcurrentHashMap.class, Integer.class, Article.class));
            String readOutput = "";
            art_id.clear();
            art_id.add(0);
            for (Integer key : received.keySet()) {
                if(art_id.contains(received.get(key).id) == false) {
                    art_id.add(received.get(key).id);
                    readOutput = readOutput + received.get(key).id + ": " + received.get(key).contents + "<br>" + "@";
                    String t = "";
                    readOutput = getReplies(received.get(key).replies, readOutput, t);
                }
            }
            String[] arrOfStr = readOutput.split("@");
            int i = 0;
            out.println("<p id=\"demo\">");
            out.println("Server:-<br><br>");
            while(i < 5 && i < arrOfStr.length) {
                out.println(arrOfStr[i]);
                i++;
            }
            out.println("</p>");
            out.println("<br><button onclick='myFunction(\""+ readOutput + "\")' id = 'btn' style='visibility:visible'>Next</button>");
            out.println("<script>\n" +
            "    var i = 5;\n" +
            "    function myFunction(readOutput) {\n" +
            "        i = i + 5;\n" +
            "        var j = i - 5;\n" +
            "        var arr = readOutput.split(\"@\");\n" +
            "        var text = \"\";\n" +
            "        while(j < i && j < arr.length) {\n" +
            "            text = text + arr[j];\n" +
            "            j++;\n" +
            "        }\n" +
            "        if(j == arr.length) {\n" +
            "            document.getElementById(\"btn\").style.visibility=\"hidden\"; \n" +
            "        }\n" +
            "        document.getElementById(\"demo\").innerHTML = text;\n" +
            "    }\n" +
            "</script>");
            }
            else if (option.equals("choose")){
                int flag = 1;
                String[] arr = receivedStr.split(";");
                String[] arr1 = receivedStr.split("Total time taken for this operation ");
                if(arr.length > 5) {
                    out.println("Article content = " + arr[5] + "<br>");
                    flag = 0;
                }
                if(arr1.length > 1) {
                    out.println("Time taken = " + arr1[1]);
                    flag = 0;
                }
                if(flag == 1) {
                    out.println("Server:-" + receivedStr);
                }
            }
            else if (option.equals("post")){
                int flag = 1;
                String[] arr1 = receivedStr.split("Total time taken for this operation ");
                if(arr1.length > 1) {
                    out.println("Time taken = " + arr1[1]);
                    flag = 0;
                }
                String[] arr = receivedStr.split(";");
                if(arr.length > 1) {
                    flag = 0;
                }
                if(flag == 1) {
                    out.println("Server:-" + receivedStr);
                }
            }
            else if (option.equals("reply")){
                int flag = 1;
                String[] arr1 = receivedStr.split("Total time taken for this operation ");
                if(arr1.length > 1) {
                    out.println("Time taken = " + arr1[1]);
                    flag = 0;
                }
                String[] arr = receivedStr.split(";");
                if(arr.length > 1) {
                    flag = 0;
                }
                if(flag == 1) {
                    out.println("Server:-" + receivedStr);
                }
            }
            else {
                out.println("Server:-" + receivedStr);
            }
            out.println("</body>");
            out.println("</html>");
        }
    }
    public static String getReplies(List<Article> replies, String readOutput, String t) 
    { 
        t = t + "&nbsp;&nbsp;&nbsp;&nbsp;" ;
        for( Article reply : replies) {
            art_id.add(reply.id);
            readOutput = readOutput + t + reply.id + ": " + reply.contents + "<br>" + "@";
            return getReplies(reply.replies, readOutput, t);
        }
        return readOutput;
    }
    public static StringBuilder data(byte[] a) 
    { 
        if (a == null) 
            return null; 
        StringBuilder ret = new StringBuilder(); 
        int i = 0; 
        while (a[i] != 0) 
        { 
            ret.append((char) a[i]); 
            i++; 
        } 
        return ret; 
    }
    

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

}