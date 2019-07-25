package litong.hyperledger.fabric.example;

import java.security.PrivateKey;

import org.hyperledger.fabric.sdk.Enrollment;

public class SampleEnrollment implements Enrollment {
	
	private String cert;
	private PrivateKey privateKey;
	
	public SampleEnrollment(String cert, PrivateKey pk) {
		this.cert = cert;
		this.privateKey = pk;
	}

	@Override
	public PrivateKey getKey() {
		return privateKey;
	}

	@Override
	public String getCert() {
		return cert;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append("cert=").append(cert).append(",");
		sb.append("privateKey=").append(privateKey).append("]");
		
		return sb.toString();
	}

}
