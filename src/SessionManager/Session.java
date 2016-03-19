package SessionManager;

import java.sql.Timestamp;
import java.util.Date;

public class Session {
	private String Id;
	private Integer version;
	private String message;
	private Date expireTime;
	
	public Session(String id, Integer version, String msg, Date expireTime){
		this.Id = id;
		this.version = version;
		this.message = msg;
		this.expireTime = expireTime;
	}

	public String getId() {
		return Id;
	}

	public void setId(String id) {
		Id = id;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Date getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(Date expireTime) {
		this.expireTime = expireTime;
	}
	
	
}
