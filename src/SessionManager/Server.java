package SessionManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello")
public class Server extends HttpServlet{
	private Hashtable<String, Session> sessionTable = new Hashtable<String,Session>();
	private final String CookieName = "CS5300PROJ1SESSION"; 
	private SessionIdGenerator sidGenerator = new SessionIdGenerator();
	private final int cookieExpireTime = 60;
	private final int sessionExpireTime = 30;
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	      throws ServletException, IOException {

		handleGetRequest(request, response);

//		new Thread(){
//			public void run(){
//				try {
//					handleGetRequest(request, response);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		}.start();
		

		 
	  }
	
	public void handleGetRequest(HttpServletRequest request, HttpServletResponse response) throws IOException{
		Map<String, String[]> map = request.getParameterMap();
		if(map == null || map.size() == 0){
			handleRefresh(request, response);
			return;
		}
		for(Entry<String, String[]> en : map.entrySet()){
			String key = en.getKey();
			if(en.getKey().equals("refresh")){
				handleRefresh(request, response);
				return;
			}else if(en.getKey().equals("replace")){
				handleReplace(request, response);
				return;
			}else if(en.getKey().equals("logOut")){
				handleLogOut(request, response);
				return;
			}
		}

		
	}
	
	private void handleRefresh(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		Cookie[] cookies = request.getCookies();
		if(cookies == null || cookies.length == 0){
//			System.out.println("cookies null");
			loginNewUser(request, response);
		}else{
			for(Cookie cookie: cookies){
				if(cookie.getName().equals(CookieName)){
//					System.out.println("old user");
//					System.out.println(cookie.getValue());
//					System.out.println(cookie.getMaxAge());	
					Session session = getSessionFromCookie(cookie);
					if(session == null){
						loginNewUser(request, response);
					}else{
						refreshOldUser(session, request, response);
					}
				}
				return;
			}
		}
	}

	private void refreshOldUser(Session session, HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		session.setVersion(session.getVersion() + 1);
		session.setExpireTime(getExpireDate(this.sessionExpireTime));
		this.sessionTable.put(session.getId(), session);
		Cookie cookie = getCookie(session);
		normalResponse(session, cookie, response);
	}

	private void normalResponse(Session session, Cookie cookie, HttpServletResponse response) throws IOException{
		response.setContentType("text/html");
		response.addCookie(cookie);
		String outPage = getOutputPage(session, cookie);
		response.setStatus(200);
	    PrintWriter out = response.getWriter();
	    out.println(outPage);
	}
	
	private void handleReplace(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		String replaceMsg = request.getParameter("refresh");
		Cookie[] cookies = request.getCookies();
		if(cookies == null || cookies.length == 0){
//			System.out.println("cookies null");
			newUserReplace(replaceMsg, request, response);
			return;
		}else{
			for(Cookie cookie: cookies){
				if(cookie.getName().equals(CookieName)){
					Session session = getSessionFromCookie(cookie);
					if(session == null){
						newUserReplace(replaceMsg, request, response);
					}else{
						OlduserReplace(replaceMsg, session, request, response);
					}
				}
				return;
			}
		}
	}
	
	private Session getSessionFromCookie(Cookie cookie) {
		// TODO Auto-generated method stub
		String[] cookieInfo = cookie.getValue().split("__");
		String sessionId = cookieInfo[1];
		String version = cookieInfo[2];
//		for(String ci : cookieInfo){
//			System.out.println(ci);
//		}
		Session session = this.sessionTable.get(sessionId);
		return session;
	}

	private void OlduserReplace(String replaceMsg, Session session, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		session.setMessage(replaceMsg);
		session.setExpireTime(getExpireDate(this.sessionExpireTime));
		session.setVersion(session.getVersion() + 1);
		sessionTable.put(session.getId(), session);
		Cookie cookie = getCookie(session);
		normalResponse(session, cookie, response);
	}

	private void newUserReplace(String replaceMsg, HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		Session session = getNewSession();
		session.setMessage(replaceMsg);
		sessionTable.put(session.getId(), session);
		Cookie cookie = getCookie(session);
		normalResponse(session, cookie, response);
	}

	private void handleLogOut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		Cookie[] cookies = request.getCookies();
		if(cookies == null || cookies.length == 0){
			replyLogOut(request, response);
			return;
		}
		for(Cookie cookie : cookies){
			if(cookie.getName().equals(this.CookieName)){
				Session session = getSessionFromCookie(cookie);
				if(session != null){
					this.sessionTable.remove(session.getId());
					replyLogOut(request, response);
					return;
				}
			}
		}
		replyLogOut(request, response);
	}





	private void replyLogOut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		response.setContentType("text/html");
		String outPage = getLogOutPage();
		response.setStatus(200);
	    PrintWriter out = response.getWriter();
	    out.println(outPage);
	}

	
	
	private String getLogOutPage() throws IOException {
		// TODO Auto-generated method stub
		BufferedReader reader = new BufferedReader(new FileReader("D:/workspace/CS5300/P1/logOut.html"));
		
		String indexContent = "";
		String line = reader.readLine();
		while(line != null){
			indexContent += line;
			line = reader.readLine();
		}	
		return indexContent;
	}

	/**
	 * handles the situation when new user login in,
	 * assign new session and add new cookie
	 * @param request
	 * @param response
	 * @throws IOException 
	 */
	private void loginNewUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		Session session = getNewSession();
		sessionTable.put(session.getId(), session);
		Cookie cookie = getCookie(session);
		normalResponse(session, cookie, response);
	}

	private String getOutputPage(Session session, Cookie cookie) throws IOException {
		// TODO Auto-generated method stub
		BufferedReader reader = new BufferedReader(new FileReader("D:/workspace/CS5300/P1/index.html"));
		
		String indexContent = "";
		String line = reader.readLine();
		while(line != null){
			indexContent += line;
			line = reader.readLine();
		}
		indexContent = indexContent.replaceAll("#SessionId#", session.getId());
		indexContent = indexContent.replaceAll("#VersionNumber#", ""+session.getVersion());
		indexContent = indexContent.replaceAll("#SessionDateInfo#", session.getExpireTime().toString());
		indexContent = indexContent.replaceAll("#cookieValue#", cookie.getValue());
		indexContent = indexContent.replaceAll("#CookieDateInfo#", getExpireDate(this.cookieExpireTime).toString());
		
		return indexContent;
	}


	private Session getNewSession(){
		String sessionId = this.sidGenerator.getSessionId();
		Integer version = 1;
		String msg = "";
	    Date expireTime = getExpireDate(this.sessionExpireTime);
		Session session = new Session(sessionId, version, msg, expireTime);
		return session;
	}
	
	private Date getExpireDate(int seconds){
		Calendar cal = Calendar.getInstance(); // creates calendar
	    cal.setTime(new Date()); // sets calendar time/date
	    cal.add(Calendar.SECOND, seconds); // adds one hour
	    Date expireTime = cal.getTime(); // returns new date object, one hour in the future
	    return expireTime;
	}
	
	
	private Cookie getCookie(Session session){
		String cookieValue = "__";
		cookieValue += session.getId();
		cookieValue += "__";
		cookieValue += session.getVersion();
		cookieValue += "__";
		Cookie cookie = new Cookie(CookieName, cookieValue);
		cookie.setMaxAge(this.cookieExpireTime);
		return cookie;
	}
	
	
	private void handleOldUser(Cookie cookie, HttpServletRequest request, HttpServletResponse response) {
		// TODO Auto-generated method stub
		String[] cookieInfo = cookie.getValue().split("__");
		String sessionId = cookieInfo[0];
		String version = cookieInfo[1];
		Session oldSession = this.sessionTable.get(sessionId);
	}

}
