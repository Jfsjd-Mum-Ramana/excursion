package com.verizon.ucs.restapi.dto;

public class AppApiResponse {
	
	private int status = 200;
	private String message = "";
    private Object data;
    private Object error;
    
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	public Object getError() {
		return error;
	}
	public void setError(Object error) {
		this.error = error;
	}
    
    
}



package com.verizon.ucs.restapi.dto;

public class ApiRequest {
	
	    private String deviceName;
	    private String model;
	    private String loopback;
	    private String vendor;
	    private String network;
	    
		public String getDeviceName() {
			return deviceName;
		}
		public void setDeviceName(String deviceName) {
			this.deviceName = deviceName;
		}
		public String getModel() {
			return model;
		}
		public void setModel(String model) {
			this.model = model;
		}
		public String getLoopback() {
			return loopback;
		}
		public void setLoopback(String loopback) {
			this.loopback = loopback;
		}
		public String getVendor() {
			return vendor;
		}
		public void setVendor(String vendor) {
			this.vendor = vendor;
		}
		public String getNetwork() {
			return network;
		}
		public void setNetwork(String network) {
			this.network = network;
		}
	    
	    

}
