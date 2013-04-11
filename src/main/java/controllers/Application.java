package controllers;

import org.joda.time.DateTime;
import com.avaje.ebean.Ebean;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import models.EditUsr;
//import models.EditFrmDat;
import models.Login;
import ninja.Context;
import models.User;
import ninja.Result;
import ninja.Results;
import ninja.ebean.NinjaEbeanModule;
import ninja.i18n.Lang;
import ninja.i18n.Messages;
import ninja.i18n.MessagesImpl;
import ninja.params.Param;
import ninja.validation.FieldViolation;
import ninja.validation.JSR303Validation;
import ninja.validation.Required;
import ninja.validation.Validation;
import ninja.validation.Validator;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Handles all general application actions like login, logout,
 * forgot password or index page
 * @author Patrick Thum 2012
 * released under Apache 2.0 License
 */
@Singleton
public class Application {

	 @Inject
	 Lang lang;
	 
	 @Inject 
	 Messages msg;
	
  public Result index() {

    return Results.html();
  }
  
  
  
//-------------------- Registration -----------------------------------
   /**
    * shows the registration form
    * @return
    */

  public Result registerForm(){  	
	  
	  return Results.html();
  }
 

  
  /**
   * Creates the User (POST for register)
   * @return
   */

public Result postRegisterForm( Context context, @JSR303Validation EditUsr frdat, Validation validation){	
	Result result = Results.html();
	String s;

	
	if(validation.hasViolations()){
		s = msg.get("msg_formerr", context, result, "String");
		context.getFlashCookie().error(s, null);

		//return the postregisterform (same as registerform, but with prepopulated data)
		return Results.html().render(frdat);
	}	
	else { //form was filled correctly, go on!
		
		if( !User.mailExists( frdat.getMail() ) ){
			// a new user, check if the passwords are matching
			
			if(frdat.getPw().equals( frdat.getPwn1() )){
			//create the user	
				User.createUser( frdat.getAsUser());
				
				s = msg.get("msg_regok", context, result, "String");
				context.getFlashCookie().success(s, null);
				
				return Results.redirect("/");
			
			} else {
				
				//password mismatch
				s = msg.get("msg_formerr", context, result, "String");
				context.getFlashCookie().error(s, null);
				return Results.redirect("/register");
							
			} 
		  }
		else{ //mailadress already exists
			System.out.println("mailex");
			//TODO should we really show this message? or rather an unspecified msg (msg_formerr)? 
			// [SEC] bruteforcing this form would expose existing mailadresses 
			s = msg.get("msg_mailex", context, result, "String");
			context.getFlashCookie().error(s, null);
			return Results.redirect("/register");
			}
		}

	}
  
  
//-------------------- Login/-out Functions -----------------------------------

  /**
   * shows the login form
   * @return the rendered login form
   */
  public Result loginForm(){
	  return Results.html();  
  }
  
  
  /**
   *  Handles the logout process
   * @return the index page
   */
  public Result logout(Context context){
	  context.getSessionCookie().clear();
	  Result result = Results.html();
	  String s = msg.get("msg_logout", context, result, "String");
	  context.getFlashCookie().success(s, null);
	  return Results.redirect("/");
  }
  
  /**
   * Handles the login-process 
   * @return the login form or the index page 
   */
  public Result loginProc(Context context, @JSR303Validation Login l, Validation validation){
		Result result = Results.html();
		String s;

		
	    //TODO return the filled form on errors due to the comfortability
	  if(validation.hasViolations()){ 
		  s = msg.get("msg_formerr", context, result, "String");
		  context.getFlashCookie().error(s, null);
		  return Results.redirect("/login");
		  
	  }
	  else{
		  User lgr = User.auth(l.getMail().toLowerCase(), l.getPwd());
		  //get the user if authentication was correct
		  if( lgr != null ){ //correct login
			  //set the cookie			  
			  context.getSessionCookie().put("id", String.valueOf(lgr.getId()));
			  context.getSessionCookie().put("usrname", lgr.getMail());
			  System.out.println(context.getSessionCookie());
			  if( lgr.isAdmin() ){
					  //also set an admin-flag if the account is an admin-account
					  context.getSessionCookie().put("adm", String.valueOf(true));
				  }
			  //TODO: ADM-Zugriff per DB, nicht per Cookie?
			  
			  s = msg.get("msg_login", context, result, "String");
			  context.getFlashCookie().success(s, null);
			  
			  return Results.redirect("/"); 
			  }
		  //TODO maybe this should go into an else-path?
		  s = msg.get("msg_formerr", context, result, "String");
		  context.getFlashCookie().error(s, null);
		  return Results.redirect("/login");
		    
	  }
  }
  
  
  
  /**
   * shows the forgot pw page
   * @return forgot-pw-form
   */
  public Result forgotPW(){
	  return Results.html();
  }
  
  /**
   * generates a new password and sends it to the user
   * @return index page
   */
  public Result pwResend(Context context,@JSR303Validation Login l, Validation validation){
		Result result = Results.html();
		String s;
		
	  if( validation.hasViolations() ) { 
		  //some fields weren't filled
		  s = msg.get("msg_formerr", context, result, "String");
		  context.getFlashCookie().error(s, (Object)null);
		  return Results.redirect("/pwresend");
		  } 
	  else {

		  User usr = User.getUsrByMail(l.getMail());
		  if( usr != null ){ //mailadress was correct
			  //generate a new pw and send it to the given mailadress
			  //TODO [SEC]IMPORTANT! if sendMail() fails, we'll get an empty String (which will be set as PW)
			  String newPw = sendMail(usr.getMail(), usr.getMail());
			  //set the new pw in the db
			  usr.setPasswd(newPw);
			  Ebean.update(usr);
			  //TODO change the msg
			  s = msg.get("forgpw_succ", context, result, "String");
			  context.getFlashCookie().success(s, (Object)null);
			  return Results.redirect("/");
			  
			   
			  }
		  //TODO missing else
		  s = msg.get("msg_formerr", context, result, "String");
		  context.getFlashCookie().error(s, (Object)null);
		  return Results.redirect("/pwresend");
	  }
	  

  }
  
  /**
   * sends the forgot-password mail to a user
   * @param mail recipient address of a user
   * @param forename name of a user for the text
   * @return the password to set it in the db
   */
  
 private String sendMail(String mail, String forename){

		
	 	  //standard-host of this application
	 	  //TODO configurable sender-address?
	 	  //String host = Play.application().configuration().getString("fpw.host");
	 	  String host = "localhost";
	      String to = mail;
	      String from = "admin@"+host;
	      Properties properties = System.getProperties();
	      properties.setProperty("mail.smtp.host", host);
	      Session session = Session.getDefaultInstance(properties);
	      	
	      try{	
	    	  //add the header-informations
		         MimeMessage message = new MimeMessage(session);
		         message.setFrom(new InternetAddress(from));
		         message.addRecipient(Message.RecipientType.TO,
		                                  new InternetAddress(to));
		         //TODO message.setSubject(Messages.get("forgpw.title"));
				  
		         message.setSubject(lang.get("forgpw_title","en"));
		         
		      //add the message body
		         String rueck = getRndPw();
		         //TODO create a better message-text
		         //message.setText(Messages.get("forgpw.msg", forename, rueck ));
		         message.setText("Hey,"+forename+"you forgot your Password, the new is:\n"+rueck);
		         Transport.send(message);
		         return rueck;
		         
	      }catch (Exception e) {
	         e.printStackTrace();
	         return "";
	      }
	}
 
 /**
  * generates a random password 
  * @return a random password with 7 characters
  */
 //TODO modify this function, also use at least digits and uppercase-letters and a variable length 
 private String getRndPw(){
	  Random rand = new Random();
	  StringBuffer strBuf = new StringBuffer();
	  
	  for (int i = 0 ; i < 7 ; i++ ) {
		  //generates a random char between a and z (an ascii a is 97, z is 122 in dec)
	  	strBuf.append( (char) ( (Math.abs( rand.nextInt() ) %26 ) +97 ) );
	  }
	  return strBuf.toString();
 }

  
}
