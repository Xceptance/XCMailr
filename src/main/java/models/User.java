package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.*;


import org.mindrot.jbcrypt.BCrypt;
import com.avaje.ebean.*;
import com.avaje.ebean.validation.Email;
import com.avaje.ebean.validation.NotNull;
/**
 * User Object
 * @author Patrick Thum 2012
 * released under Apache 2.0 License
 */
@Entity
public class User {
	//UserId
	@Id
	private long id;
	
	@NotNull
	//Forename of the User
	private String forename;
	
	@NotNull
	//Surname of the User
	private String surname;
	//Mailadress
	
	@Email
	private String mail;
	
	@NotNull
	//Password
	private String passwd;
	
	//Admin-Flag
	private boolean admin; 
	
	

	//Relation to the Mailboxes 
	@OneToMany(mappedBy="usr", cascade=CascadeType.ALL)
    public List<MBox> boxes;
//    //Finder
//	public static Finder<Long,User> find = new Finder(Long.class, User.class);
//	
	// ----------------------------- Getter and Setter ------------------------
	/**
	 * Standard constructor, just initialize the variables
	 */
	public User(){
		id = 0;
		forename = "";
		surname = "";
		mail = "";
		passwd = "";
		boxes = new ArrayList<MBox>();
	}
	
	/**
	 * constructor for the userobject
	 * @param fName forename
	 * @param sName surname
	 * @param eMail mail
	 * @param pw password
	 */
	public User(String fName, String sName, String eMail, String pw){
		id = 0;
		setForename(fName);
		setSurname(sName);
		setMail(eMail);
		hashPasswd(pw);
		setAdmin(false);
		boxes = new ArrayList<MBox>();
	}
	
	
	
	
	public List<MBox> getBoxes() {
		return boxes;
	}

	public void setBoxes(List<MBox> boxes) {
		this.boxes = boxes;
	}

	/**
	 * persists a user in the DB
	 * @param u the user
	 */
	public static void createUser(User u){
		u.setMail(u.getMail().toLowerCase());
		Ebean.save(u);
		
	}
	
	/**
	 * @return the Id of a user
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets the id of a Userobject used to identify it in the db
	 * @param id 
	 */
	public void setId(long id) {
		this.id = id;
		
	}
	/**
	 * @return the forename of a user
	 */
	public String getForename() {
		return forename;
	}
	/**
	 * Sets the forename of a user
	 * @param forename
	 */
	public void setForename(String forename) {
		this.forename = forename;
	}
	/**
	 * @return the surname of a user
	 */
	public String getSurname() {
		return surname;
	}
	/**
	 * Sets the surname of a user
	 * @param surname
	 */
	public void setSurname(String surname) {
		this.surname = surname;
	}
	/**
	 * @return the real Mailadress of a user
	 */
	public String getMail() {
		return mail;
	}
	/**
	 * Sets the real Mailadress of a user
	 * @param mail
	 */
	public void setMail(String mail) {
		this.mail = mail.toLowerCase();
	}
	/**
	 * returns the hashvalue of the bcrypted password
	 * @return
	 */
	public String getPasswd() {
		
		return passwd;
	}
	
	public void hashPasswd(String passwd){
		setPasswd(BCrypt.hashpw(passwd, BCrypt.gensalt()));
	}
	/**
	 * sets the given password
	 * WARNING: don't use this method to set a new password!
	 * always use  hashPasswd() for that!
	 * @param passwd
	 */
	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}
	
	/**
	 * @return true if a user is admin 
	 */
	public boolean isAdmin() {
		return admin;
	}
	/**
	 * enables or disables the admin-functions
	 * @param admin: enables functions if true
	 */
	public void setAdmin(boolean admin) {
		this.admin = admin;
	}
	/**
	 * @return the list of all users in the database
	 */
	public static List<User> all() {
		return Ebean.find(User.class).findList();
		//return find.all();
	}
	
	
	//---------------------------- EBean/Finder-Functions----------------------
	/**
	 * checks if a mailadress exists in the database
	 * @param mail: mailadress of a user
	 * @return true if the given adress exists
	 */	
	public static boolean mailExists(String mail){
		if( !Ebean.find(User.class).where().eq("mail", mail.toLowerCase()).findList().isEmpty() ){
			return true;
		}else{ return false; }
	}
	
	
	/**
	 * 
	 * @param mail
	 * @param uId
	 * @return
	 */
	//rewrote mailExists() for Editing MBoxes & Users
	
	//TODO FIX this function
	public static boolean mailExists(String mail, Long uId){
		
		//get all users with the given mail
		List<User> ul = Ebean.find(User.class).where().eq( "mail", mail.toLowerCase() ).findList();
		
		if( !ul.isEmpty() ){
			//there are some users with that mailadress
			
			if( (ul.size() == 1) && (ul.get(0).getId() == uId) ){
				//there's only one user and the given UID is equal to the UID in the db
				return false;
				}
			else{
				//there's more than one user with that address and/or 
				//the UID belongs to another user
				return true; 
				}
			} 
		else {
			// there's no user with that address
			return false;
			}
	}
	
	/**
	 * returns the user-object that belongs to the given mailadress
	 * @param mail: adress of a user
	 * @return the user
	 */
	public static User getUsrByMail(String mail){
		return Ebean.find(User.class).where().eq("mail", mail.toLowerCase()).findUnique();		
	}
	
	/**
	 * 
	 * @param mail
	 * @param pw
	 * @return the user object if the given mail and password belong together 
	 */

	public static User auth(String mail, String pw){
		//get the user by the mailadress
		User usr = Ebean.find(User.class).where().eq("mail", mail.toLowerCase()).findUnique();
		
		if( !(usr == null) ){
			//there's a user with that address
			
			
			// TODO next code is redundant, same like authById()
			if(BCrypt.checkpw( pw, usr.getPasswd())){
				
				//check if the given password is correct
				return usr;
			}
			else {
				//the password is wrong
				
				return null;
			}
		}
		else {
			//there's no user with that address
			
			return null; 
			}
	}
	
	/**
	 * 
	 * @param id
	 * @param pw
	 * @return the user object if the given userId and password belong together 
	 */
	
	public static User authById(Long id, String pw){	
		User usr = Ebean.find(User.class).where().eq("id", id).findUnique();
		if( BCrypt.checkpw( pw, usr.getPasswd() ) ){
			return usr;
		}
		else {
			
			return null;
		}
	}
	/**
	 * 
	 * @param id: a users id
	 * @return the user-object  
	 */
	public static User getById(Long id){
		return Ebean.find(User.class).where().eq("id", id).findUnique();
	}
	
	/**
	 * updates the data of a user
	 * @param usr: the edited user-object
	 */
	
	public static void updateUser(User usr){
		Ebean.update( usr );
	}

	/**
	 * deletes the user with the given id 
	 * @param id: the id of the user that has to be deleted
	 */

	public static void delete(Long id) {
		Ebean.delete(User.class, id);
	}
	
	
	/**
	 * promotes the User and Updates the DB 
	 * @param id
	 */
	public static void promote(Long id) {
		User usr = Ebean.find(User.class).where().eq("id", id).findUnique();
		usr.setAdmin( !usr.isAdmin() );
		Ebean.update( usr );
	}
	public String toString(){
		return id+" "+forename+" "+" "+surname+" "+mail+" "+passwd;
	}
	
}
