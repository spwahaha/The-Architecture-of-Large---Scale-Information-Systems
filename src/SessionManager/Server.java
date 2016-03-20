package SessionManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello")
public class Server extends HttpServlet{
	private ConcurrentHashMap <String, Session> sessionTable = new ConcurrentHashMap<String,Session>();
	private final String CookieName = "CS5300PROJ1SESSION"; 
	private SessionIdGenerator sidGenerator;
	private final int cookieExpireTime = 60;
	private final int sessionExpireTime = 30;
	ScheduledExecutorService gc;
	
	/**
	 * Initiate the server and start the garbage collection thread
	 */
	@Override
	public void init() {
		sidGenerator = new SessionIdGenerator();
		gc = Executors.newSingleThreadScheduledExecutor();
		gc.scheduleAtFixedRate(new Runnable() {
		  @Override
		  public void run() {
			  Set<Entry<String, Session>> entries = sessionTable.entrySet();
			  for(Entry entry : entries){
				  if(isExpired((Session)entry.getValue())){
					  sessionTable.remove(entry.getKey());
					  System.out.println(entry.getKey() + "removed");
				  }
			  }
			  System.out.println("Hashtable cleaned");
		  }
		}, 0, 30, TimeUnit.SECONDS);
	}
	
	/**
	 * @param session
	 * @return whether the session has expired
	 */
	private boolean isExpired(Session session){
		return getExpireDate(0).after(session.getExpireTime());
	}
	
	/**
	 * Destroy the garbage collection thread when server shut down
	 */
	@Override
	public void destroy() {
		gc.shutdown();
	}
	
	/**
	 * Handle get method action
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	      throws ServletException, IOException {

		handleGetRequest(request, response);	 

	}
	
	/**
	 * Handle get action
	 * @param request
	 * @param response
	 * @throws IOException
	 */
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
	
	/**
	 * Handle refresh request
	 * @param request
	 * @param response
	 * @throws IOException
	 */
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
						Date cur = getExpireDate(0);
						if(isExpired(session)){
							// session expired
							loginNewUser(request, response);
							return;
						}else{
							refreshOldUser(session, request, response);
						}
					}
				}
				return;
			}
		}
	}

	/**
	 * Refresh the user that exists in the session manager
	 * @param session
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void refreshOldUser(Session session, HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		session.setVersion(session.getVersion() + 1);
		session.setExpireTime(getExpireDate(this.sessionExpireTime));
		this.sessionTable.put(session.getId(), session);
		Cookie cookie = getCookie(session);
		normalResponse(session,cookie, request, response);
	}

	/**
	 * Output the index html page
	 * @param session
	 * @param cookie
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void normalResponse(Session session, Cookie cookie, HttpServletRequest request, HttpServletResponse response) throws IOException{
		response.setContentType("text/html");
		response.addCookie(cookie);
		String outPage = getOutputPage(session, cookie);
		response.setStatus(200);
		PrintWriter out = response.getWriter();
		out.println(outPage);
	}
	
	/**
	 * Handle replace request
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void handleReplace(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		String replaceMsg = request.getParameter("replaceInfo");
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
	
	/**
	 * get the session according to the cookie info
	 * Null if the session has expired or the session doesn't exist
	 * @param cookie
	 * @return Session based on cookie information
	 */
	private Session getSessionFromCookie(Cookie cookie) {
		// TODO Auto-generated method stub
		String[] cookieInfo = cookie.getValue().split("__");
		String sessionId = cookieInfo[1];
		String version = cookieInfo[2];
		Session session = this.sessionTable.get(sessionId);
		return session;
	}

	/**
	 * Handle the replace request for the user that exist
	 * @param replaceMsg
	 * @param session
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void OlduserReplace(String replaceMsg, Session session, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		session.setMessage(replaceMsg);
		session.setExpireTime(getExpireDate(this.sessionExpireTime));
		session.setVersion(session.getVersion() + 1);
		sessionTable.put(session.getId(), session);
		Cookie cookie = getCookie(session);
		normalResponse(session, cookie, request, response);
	}
	
	/**
	 * Handle the replace request for the session
	 * that already expired
	 * @param replaceMsg
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void newUserReplace(String replaceMsg, HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		Session session = getNewSession();
		session.setMessage(replaceMsg);
		sessionTable.put(session.getId(), session);
		Cookie cookie = getCookie(session);
		normalResponse(session, cookie, request, response);
	}

	/**
	 * Handle logOur request, reply same information for
	 * the user that still in the system or has expired 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
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

	/**
	 * Replay the log out html page
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void replyLogOut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		response.setContentType("text/html");
		String outPage = getLogOutPage();
		response.setStatus(200);
	    PrintWriter out = response.getWriter();
	    out.println(outPage);
	}

	
	/**
	 * Get the logout html content
	 * @return the logout html content
	 * @throws IOException
	 */
	private String getLogOutPage() throws IOException {
		// TODO Auto-generated method stub
		String path = getServletContext().getRealPath("/resources/logOut.html");
		BufferedReader reader = new BufferedReader(new FileReader(path));
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
		normalResponse(session, cookie, request, response);
	}
	
	/**
	 * Get the index html content
	 * @param session
	 * @param cookie
	 * @return the index html content
	 * @throws IOException
	 */
	private String getOutputPage(Session session, Cookie cookie) throws IOException {
		// TODO Auto-generated method stub
		String path = getServletContext().getRealPath("/resources/index.html");
		BufferedReader reader = new BufferedReader(new FileReader(path));
		String indexContent = "";
		String line = reader.readLine();
		while(line != null){
			indexContent += line;
			line = reader.readLine();
		}
		indexContent = indexContent.replaceAll("#SessionMsg#", Matcher.quoteReplacement(escapeHTMLentities(session.getMessage())));
		indexContent = indexContent.replaceAll("#SessionId#", session.getId());
		indexContent = indexContent.replaceAll("#VersionNumber#", ""+session.getVersion());
		indexContent = indexContent.replaceAll("#SessionDateInfo#", session.getExpireTime().toString());
		indexContent = indexContent.replaceAll("#cookieValue#", cookie.getValue());
		indexContent = indexContent.replaceAll("#CookieDateInfo#", getExpireDate(this.cookieExpireTime).toString());
		return indexContent;
	}
	
	/**
	 * To escape the html entities for the replace message
	 * @param msg the original msg
	 * @return the entity safe html message
	 */
	private String escapeHTMLentities(String msg){
		msg = msg.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;");
		return msg.replaceAll("[$]", "\\$");
	}

	/**
	 * Generate a new session 
	 * @return new session
	 */
	private Session getNewSession(){
		String sessionId = this.sidGenerator.getSessionId();
		Integer version = 1;
	    Date expireTime = getExpireDate(this.sessionExpireTime);
		Session session = new Session(sessionId, version, expireTime);
		return session;
	}
	
	/**
	 * Get the expire time based on expire interval and current time
	 * ExpireTime = CurrentTime + Interval(seconds)
	 * @param interval the interval of expire, unit: second
	 * @return
	 */
	private Date getExpireDate(int interval){
		Calendar cal = Calendar.getInstance(); // creates calendar
	    cal.setTime(new Date()); // sets calendar time/date
	    cal.add(Calendar.SECOND, interval); // adds one hour
	    Date expireTime = cal.getTime(); // returns new date object, one hour in the future
	    return expireTime;
	}

	/**
	 * Generate the cookie based on the session information
	 * @param session the session that is used to generte cookie
	 * @return the cookie for the session
	 */
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
	
}
