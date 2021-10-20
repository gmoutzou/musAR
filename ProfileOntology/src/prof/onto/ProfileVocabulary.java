package prof.onto;

public interface ProfileVocabulary {

	// -------> Basic vocabulary
	public static final int CREATE_PROFILE = 1;
	public static final int READ_PROFILE = 2;
	public static final int UPDATE_PROFILE = 3;
	public static final int DELETE_PROFILE = 4;
	public static final int OTHER_OPERATION = 5;
	public static final int SUCCESS = 200;
	public static final int ERROR = 400;
	public static final String PROFILE_MANAGEMENT = "profile-management";
	public static final String DB_MANAGEMENT = "database-management";
	public static final String OK = "OK";
	public static final String UNKNOWN_ERROR  = "Unknown error";
	public static final String ACCOUNT_NOT_FOUND  = "Account not found";
	public static final String ILLEGAL_OPERATION  = "Illegal operation";

	// -------> Ontology vocabulary
	public static final String PROFILE = "Profile";
	public static final String PROFILE_ACCOUNT = "account";
	public static final String PROFILE_NAME = "name";

	public static final String MAKE_OPERATION = "MakeOperation";
	public static final String MAKE_OPERATION_TYPE = "type";
	public static final String MAKE_OPERATION_ACCOUNT = "account";
	public static final String MAKE_OPERATION_USER_CHOICE = "userChoice";
	
	public static final String MAKE_DB_OPERATION = "MakeDBOperation";
	public static final String MAKE_DB_OPERATION_TYPE = "type";
	public static final String MAKE_DB_OPERATION_PROFILE = "profile";

	public static final String INFORMATION = "Information";
	public static final String INFORMATION_TYPE = "type";
	public static final String INFORMATION_PROFILE = "profile";
	public static final String INFORMATION_STATUS = "status";

	public static final String STATUS = "Status";
	public static final String STATUS_CODE = "code";
	public static final String STATUS_MESSAGE = "message";

}
