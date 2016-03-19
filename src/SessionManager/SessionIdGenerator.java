package SessionManager;

public class SessionIdGenerator {
	int next;
	public SessionIdGenerator(){
		this.next = 0;
	}
	
	public String getSessionId(){
		this.next++;
		return ""+this.next;
	}
}
