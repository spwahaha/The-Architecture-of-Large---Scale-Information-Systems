package SessionManager;

import java.util.UUID;

public class SessionIdGenerator {
	int next;
	public SessionIdGenerator(){
		this.next = 0;
	}
	
//	public String getSessionId(){
//		this.next++;
//		return ""+this.next;
//	}
	
	public String getSessionId(){
		return UUID.randomUUID().toString(); 
	}
}
