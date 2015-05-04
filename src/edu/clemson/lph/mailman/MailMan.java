package edu.clemson.lph.mailman;
/*
Copyright 2014 Michael K Martin

This file is part of Civet.

Civet is free software: you can redistribute it and/or modify
it under the terms of the Lesser GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Civet is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the Lesser GNU General Public License
along with Civet.  If not, see <http://www.gnu.org/licenses/>.
*/
import java.util.*;
import java.io.*;

import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;

import org.apache.log4j.Logger;

import edu.clemson.lph.civet.Civet;


public class MailMan {
	private static final Logger logger = Logger.getLogger(Civet.class.getName());
	private static String sUserID = null;
	private static String sPassword = null;
	private static String sHost = null;
	private static int iPort = -1;
	private static String sFrom = null;
	private static String sSecurity = "NONE";

	public MailMan() {

	}

	/**
	 * This version sends a single disk file by full path
	 * @param sTo
	 * @param sCC
	 * @param sSubject
	 * @param sMsg
	 * @param sFilePath
	 * @return
	 * @throws MessagingException 
	 */
	public static boolean sendIt(String sTo, String sCC, String sSubject, String sMsg, String sFilePath)
	   throws AuthenticationFailedException, MessagingException	{
		boolean bRet = true;
		Properties props = setupProperties();
		try
		{
			Authenticator auth = new Authenticator() {
				public PasswordAuthentication getPasswordAuthentication()
				{
					return new PasswordAuthentication(sUserID, sPassword);
				}
			};
			Session session = Session.getInstance(props, auth);

			MimeMessage msg = new MimeMessage(session);
			// Create the message part
			BodyPart messageBodyPart = new MimeBodyPart();
			// Fill the message
			messageBodyPart.setText(sMsg);
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			// Part two is attachment
			messageBodyPart = new MimeBodyPart();
			FileDataSource source = new FileDataSource( sFilePath );
			messageBodyPart.setDataHandler(new DataHandler(source));
			File fBody = new File( sFilePath );
			String sFileName = fBody.getName();
			messageBodyPart.setFileName(sFileName);
			multipart.addBodyPart(messageBodyPart);
			// Put parts in message
			msg.setContent(multipart);
			msg.setSubject(sSubject);
			msg.setFrom(new InternetAddress(sFrom));
			if( sTo.contains(",") ) {
				StringTokenizer tok = new StringTokenizer(sTo,",");
				while( tok.hasMoreTokens() ) {
					String sNextTo = tok.nextToken();
					if( sNextTo.contains("@") )
					  msg.addRecipient(Message.RecipientType.TO, new InternetAddress(sNextTo));
				}
			}
			else {
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress(sTo));
			}
			if( sCC != null && sCC.trim().length() > 0 && sCC.contains("@") )
			    msg.addRecipient(Message.RecipientType.CC, new InternetAddress(sCC));
			Transport.send(msg);
		}
		catch( javax.mail.AuthenticationFailedException auth ) {
			throw auth;
		}
		catch( javax.mail.MessagingException mex ) {
			throw mex;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			bRet = false;
		}
		return bRet;
	}

	/**
	 * This version sends an email with a list of disk files.
	 * @param sTo
	 * @param sSubject
	 * @param sMsg
	 * @param aFilePaths
	 * @return
	 * @throws AuthenticationFailedException, MessagingException 
	 */
	public static boolean sendDiskFiles(String sTo, String sCC, String sSubject, String sMsg, ArrayList<String> aFilePaths)
			throws AuthenticationFailedException, MessagingException	{
		boolean bRet = true;
		Properties props = setupProperties();
		try
		{
			Authenticator auth = new Authenticator() {
				public PasswordAuthentication getPasswordAuthentication()
				{
					return new PasswordAuthentication(sUserID, sPassword);
				}
			};
			Session session = Session.getInstance(props, auth);

			MimeMessage msg = new MimeMessage(session);
			// Create the message part
			BodyPart messageBodyPart = new MimeBodyPart();
			// Fill the message
			messageBodyPart.setText(sMsg);
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			// Part two is attachments
			for( String sFilePath : aFilePaths ) {
				messageBodyPart = new MimeBodyPart();
				FileDataSource source = new FileDataSource( sFilePath );
				messageBodyPart.setDataHandler(new DataHandler(source));
				File fBody = new File( sFilePath );
				String sFileName = fBody.getName();
				messageBodyPart.setFileName(sFileName);
			}
			multipart.addBodyPart(messageBodyPart);
			// Put parts in message
			msg.setContent(multipart);
			msg.setSubject(sSubject);
			msg.setFrom(new InternetAddress(sFrom));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(sTo));
			if( sCC != null && sCC.trim().length() > 0 && sCC.contains("@") )
				msg.addRecipient(Message.RecipientType.CC, new InternetAddress(sCC));
			Transport.send(msg);
		}
		catch( javax.mail.AuthenticationFailedException auth ) {
			throw auth;
		}
		catch( javax.mail.MessagingException mex ) {
			throw mex;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			bRet = false;
		}
		return bRet;
	}

	/**
	 * This version sends an email with a list of MIME encoded attachments.
	 * It is used when the files do not actually exist on disk but as byte[] and
	 * file names.
	 * @param sTo
	 * @param sCC
	 * @param sSubject
	 * @param sMsg
	 * @param aFiles MIMEFile arrayList 
	 * @return
	 * @throws javax.mail.AuthenticationFailedException
	 */
	public static boolean sendIt(String sTo, String sCC, String sSubject, String sMsg,
			ArrayList<MIMEFile> aFiles)
					throws AuthenticationFailedException, MessagingException {
		boolean bRet = true;
		if( aFiles == null || aFiles.size() <= 0 ) {
			logger.warn("No Files to Send");
			return false;
		}
		Properties props = setupProperties();
		try
		{
			Authenticator auth = new Authenticator() {
				public PasswordAuthentication getPasswordAuthentication()
				{
					return new PasswordAuthentication(sUserID, sPassword);
				}
			};
			Session session = Session.getInstance(props, auth);

			MimeMessage msg = new MimeMessage(session);
			// Create the message part
			BodyPart messageBodyPart = new MimeBodyPart();
			// Fill the message
			messageBodyPart.setText(sMsg);
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			// Part two is attachments
			for( MIMEFile mFile : aFiles ) {
				String sFileName = mFile.getFileName();
				String sFileType = mFile.getMimeType();
				byte[] bytes = mFile.getFileBytes();
				messageBodyPart = new MimeBodyPart();
				ByteArrayDataSource source = new ByteArrayDataSource(bytes, sFileType);
				messageBodyPart.setDataHandler(new DataHandler(source));
				messageBodyPart.setFileName(sFileName);
				multipart.addBodyPart(messageBodyPart);
			}
			// Put parts in message
			msg.setContent(multipart);
			msg.setSubject(sSubject);
			msg.setFrom(new InternetAddress(sFrom));
			if( sTo.contains(",") ) {
				StringTokenizer tok = new StringTokenizer(sTo,",");
				while( tok.hasMoreTokens() ) {
					String sNextTo = tok.nextToken();
					if( sNextTo.contains("@") )
						msg.addRecipient(Message.RecipientType.TO, new InternetAddress(sNextTo));
				}
			}
			else {
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress(sTo));
			}
			if( sCC != null && sCC.trim().length() > 0 && sCC.contains("@") )
				msg.addRecipient(Message.RecipientType.CC, new InternetAddress(sCC));
			Transport.send(msg);
		}
		catch( javax.mail.AuthenticationFailedException auth ) {
			throw auth;
		}
		catch( javax.mail.MessagingException mex ) {
			throw mex;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			bRet = false;
		}
		return bRet;
					}

	private static Properties setupProperties() {
		Properties props = new Properties();
		// Works with newer email such as gmail
		if( "STARTTLS".equalsIgnoreCase(sSecurity) ) {
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.host", sHost);
			props.put("mail.smtp.port", iPort);
		}
		// Works with older SMTP over SSL
		else if( "SSL".equalsIgnoreCase(sSecurity) ) {
			props.put("mail.smtp.user", sUserID);
		    props.put("mail.smtp.host", sHost);
		    props.put("mail.smtp.port", iPort);
		    props.put("mail.smtp.starttls.enable","true");
		    props.put("mail.smtp.debug", "true");
		    props.put("mail.smtp.auth", "true");
		    props.put("mail.smtp.socketFactory.port", iPort);
		    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		    props.put("mail.smtp.socketFactory.fallback", "false");
		}
		// Works with open port 25! yikes
		else {
			props.put("mail.smtp.user", sUserID);
			props.put("mail.smtp.host", sHost);
			props.put("mail.smtp.port", iPort);
			props.put("mail.transport.protocol","smtp");
			props.put("mail.smtp.socketFactory.port", iPort);
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.enable","false");
			props.put("mail.smtp.socketFactory.fallback", "true");
		}
		System.getSecurityManager();
		return props;
	}

	public static void setDefaultUserID(String sUserID) {
		MailMan.sUserID = sUserID;
	}

	public static String getDefaultUserID() {
		return sUserID;
	}

	public static void setDefaultPassword(String sPassword) {
		MailMan.sPassword = sPassword;
	}

	public static String getDefaultPassword() {
		return sPassword;
	}

	public static void setDefaultHost(String sHost) {
		MailMan.sHost = sHost;
	}

	public static String getDefaultHost() {
		return sHost;
	}

	public static void setDefaultPort(int iPort) {
		MailMan.iPort = iPort;
	}

	public static void setSecurity(String sSecurity) {
		MailMan.sSecurity = sSecurity;
	}

	public static String getSecurity() {
		return sSecurity;
	}

	public static int getDefaultPort() {
		return iPort;
	}

	public static void setDefaultFrom(String sFrom) {
		MailMan.sFrom = sFrom;
	}

	public static String getDefaultFrom() {
		return sFrom;
	}

}
