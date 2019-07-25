package litong.hyperledger.fabric.example;

import java.io.Serializable;
import java.util.Set;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;

public class SampleUser implements User, Serializable {

	private static final long serialVersionUID = -5211650800690285091L;
	
	private String name;
	private String mspId;
	private String affiliation;
	private SampleEnrollment enrollment;
	private String account;
	private Set<String> roles;

	public SampleUser(String name, String mspId, String affiliation, SampleEnrollment enrollment) {
		this.name = name;
		this.mspId = mspId;
		this.affiliation = affiliation;
		this.enrollment = enrollment;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<String> getRoles() {
		return roles;
	}

	@Override
	public String getAccount() {
		return account;
	}

	@Override
	public String getAffiliation() {
		return affiliation;
	}

	@Override
	public Enrollment getEnrollment() {
		return enrollment;
	}

	@Override
	public String getMspId() {
		return mspId;
	}

}
