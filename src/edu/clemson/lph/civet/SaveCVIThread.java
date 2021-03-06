package edu.clemson.lph.civet;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import edu.clemson.lph.civet.lookup.LocalPremisesTableModel;
import edu.clemson.lph.civet.lookup.PremisesLocalStore;
import edu.clemson.lph.civet.lookup.PurposeLookup;
import edu.clemson.lph.civet.lookup.VetLookup;
import edu.clemson.lph.civet.xml.CviMetaDataXml;
import edu.clemson.lph.civet.xml.StdeCviXml;
import edu.clemson.lph.civet.xml.StdeCviXmlBuilder;
import edu.clemson.lph.dialogs.MessageDialog;
import edu.clemson.lph.dialogs.ProgressDialog;
import edu.clemson.lph.utils.FileUtils;
import edu.clemson.lph.utils.IDTypeGuesser;

public class SaveCVIThread extends Thread {
	private static final Logger logger = Logger.getLogger(Civet.class.getName());
	private static final long MAX_SANE_SIZE = 5000000;
	private String sCVINbrSource = CviMetaDataXml.CVI_SRC_CIVET;
	private CivetEditDialog dlg;
	private ProgressDialog prog;
	private StdeCviXml stdXml = null;
	private byte bAttachmentBytes[] = null;
	private String sAttachmentFileName; // Either original filename or same as email
	private byte bAttachmentFileBytes[] = null;
	private String sOriginalFileName = null;
	private File fOriginalFile = null; // Original file IF it is to be stored in message
	private String sXmlFileName;
	private boolean bImport;
	private String sOriginStateCode;
	private String sOriginPIN;
	private String sOriginName ;
	private String sOriginAddress;
	private String sOriginCity;
	private String sOriginZipCode;
	private String sOriginPhone;
	private String sDestinationStateCode;
	private String sDestinationPIN;
	private String sDestinationName;
	private String sDestinationAddress;
	private String sDestinationCity;
	private String sDestinationZipCode;
	private String sDestinationPhone;
	private java.util.Date dDateIssued;
	private java.util.Date dDateReceived;
	
	@SuppressWarnings("unused") // this just gets converted to a sStdPurpose but keep for debugging, etc.
	private String sMovementPurpose;
	private String sStdPurpose;
	private Integer iIssuedByKey;
	private String sIssuedByName;
	private String sCVINo;
	private ArrayList<SpeciesRecord> aSpecies;
	private ArrayList<String> aErrorKeys;
	private ArrayList<AnimalIDRecord> aAnimalIDs;
	private String sErrorNotes;
	private boolean bNoEmail;
	private boolean bCancel = false;
	private boolean bXFA = false;

	public SaveCVIThread(CivetEditDialog dlg, StdeCviXml stdXmlIn,
			byte[] bAttachmentBytesIn, String sOriginalFileName, File fOriginalFileIn, boolean bImport, boolean bXFAIn,
			String sOtherStateCode, String sOtherName, String sOtherAddress, String sOtherCity, String sOtherZipcode, String sOtherPIN,
			String sThisPIN, String sThisName, String sPhone,
			String sThisAddress, String sThisCity, String sZipcode,
			java.util.Date dDateIssued, java.util.Date dDateReceived, Integer iIssuedByKey, String sIssuedByName, String sCVINo,
			String sMovementPurpose,
			ArrayList<SpeciesRecord> aSpeciesIn,
			ArrayList<String> aErrorKeysIn, String sErrorNotes,
			ArrayList<AnimalIDRecord> aAnimalIDs) {
		this.bImport = bImport;
		this.dlg = dlg;
		prog = new ProgressDialog(dlg, "Civet", "Saving CVI");
		prog.setAuto(true);
		prog.setVisible(true);
		this.stdXml = stdXmlIn;
		this.bAttachmentBytes = bAttachmentBytesIn;
		this.sOriginalFileName = sOriginalFileName;
		this.fOriginalFile = fOriginalFileIn;
		this.dDateIssued = dDateIssued;
		this.dDateReceived = dDateReceived;
		this.iIssuedByKey = iIssuedByKey;
		this.sIssuedByName = sIssuedByName;
		this.sMovementPurpose = sMovementPurpose;
		PurposeLookup purpose = new PurposeLookup( sMovementPurpose );
		this.sStdPurpose = purpose.getUSAHACode();
		if( sStdPurpose == null || sStdPurpose.trim().length() == 0 )
			sStdPurpose = "other";
		this.sCVINo = sCVINo;
		if( bImport ) {
			this.sOriginPIN = sOtherPIN;
			this.sOriginName = sOtherName;
			this.sOriginAddress = sOtherAddress;
			this.sOriginStateCode = sOtherStateCode;
			this.sOriginCity = sOtherCity;
			this.sOriginZipCode = sOtherZipcode;
			this.sOriginPhone = null;
			this.sDestinationPIN = sThisPIN;
			this.sDestinationName = sThisName;
			this.sDestinationAddress = sThisAddress;
			this.sDestinationCity = sThisCity;
			this.sDestinationStateCode = CivetConfig.getHomeStateAbbr();
			this.sDestinationZipCode = sZipcode;
			this.sDestinationPhone = sPhone;
		}
		else {
			this.sOriginPIN = sThisPIN;
			this.sOriginName = sThisName;
			this.sOriginAddress = sThisAddress;
			this.sOriginStateCode = CivetConfig.getHomeStateAbbr();
			this.sOriginCity = sThisCity;
			this.sOriginZipCode = sZipcode;
			this.sOriginPhone = sPhone;
			this.sDestinationPIN = sOtherPIN;
			this.sDestinationName = sOtherName;
			this.sDestinationAddress = sOtherAddress;
			this.sDestinationCity = sOtherCity;
			this.sDestinationStateCode = sOtherStateCode;
			this.sDestinationZipCode = sOtherZipcode;
			this.sDestinationPhone = null;
		}
		 // Deep copy aSpecies to avoid thread issues.  (do the same for update later)
		this.aSpecies = new ArrayList<SpeciesRecord>();
		if( aSpeciesIn != null ) // Should NEVER be null
			for( Iterator<SpeciesRecord> iter = aSpeciesIn.iterator(); iter.hasNext(); )
				this.aSpecies.add( iter.next() );
		this.aErrorKeys = new ArrayList<String>();
		if( aErrorKeysIn != null )
			for( String sErrorKey : aErrorKeysIn )
				this.aErrorKeys.add( sErrorKey );
		this.sErrorNotes = sErrorNotes;
		this.aAnimalIDs = new ArrayList<AnimalIDRecord>();
		if( aAnimalIDs != null )
			for( AnimalIDRecord rID : aAnimalIDs )
				this.aAnimalIDs.add( rID );
		this.bXFA = bXFAIn;
	}


	@Override
	public void run() {
		
		try {
			setUpFileNamesAndContent();
			if( bCancel ) return;
			String sXml = buildXml();
			saveXml( sXml );
			if( bCancel ) return;
			if( !bNoEmail )
				saveEmail( sXml );
			if( CivetConfig.isStandAlone() ) {
				saveLocalPremData();
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			logger.error(ex);
		}
	    finally {
	    	
	    }
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dlg.saveComplete();
				prog.setVisible(false);
				prog.dispose();
			}
		});
	}

	private void saveLocalPremData() {
		PremisesLocalStore dataStore = LocalPremisesTableModel.getLocalStore();
		if( bImport )
			dataStore.addPremises(  sDestinationPIN,  sDestinationName,  sDestinationAddress,  sDestinationCity,			
					sDestinationStateCode,  sDestinationZipCode,  sDestinationPhone );
		else
			dataStore.addPremises(  sOriginPIN,  sOriginName,  sOriginAddress,  sOriginCity,			
					sOriginStateCode,  sOriginZipCode,  sOriginPhone );
	}

	private void setUpFileNamesAndContent() {
		// In the Web Services version we want to use potentially two different file types.
		// For the attachment in USAHerds we want the original file if not PDF or is XFA.  
		// For email, we want a PDF made from whatever image format was sent.  NOTE: for
		// SC ours start as PDF but other users may scan to tiff, etc.
		
		// First case, the original file was an XFA PDF
		if( bAttachmentBytes == null && fOriginalFile != null ) {
			long len = fOriginalFile.length();
			FileInputStream r;
			try {
				r = new FileInputStream( fOriginalFile );
				byte bOriginalFileBytes[] = new byte[(int)len];
				int iRead = r.read(bOriginalFileBytes);
				r.close();
				if( iRead != len ) {
					throw new IOException( "Array length "+ iRead + " does not match file length " + len);
				}
				bAttachmentFileBytes = bOriginalFileBytes;
				// Use original filename unless there isn't one or it is not .pdf
				if( sAttachmentFileName == null ) {
					sAttachmentFileName = fOriginalFile.getName();
				}
			} catch (IOException e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						MessageDialog.showMessage( dlg, "Civet: Error",
								"Error Reading PDF File " + fOriginalFile.getAbsolutePath() )	;
						prog.setVisible(false);
						prog.dispose();
					}
				});
				bCancel = true;
				return;
			}
		}
		// In this case we use the filename and bytes from a previously saved eCVI
		else if( bAttachmentBytes != null && sOriginalFileName != null ) {
			sAttachmentFileName = sOriginalFileName;
			bAttachmentFileBytes = bAttachmentBytes;
		}
		// This applies to the original scenario where we extract pages and save as individual PDFs.
		// Because the standard expects attached images to be PDF, we'll save even other image formats
		// this way.  
		else if( bAttachmentBytes != null && sAttachmentFileName == null ) {
			sAttachmentFileName = "CVI_" + sOriginStateCode + "_To_" + sDestinationStateCode + "_" + sCVINo + ".pdf";
			bAttachmentFileBytes = bAttachmentBytes;
		}
		if( bAttachmentFileBytes == null || sAttachmentFileName == null )
			logger.error( "Reached end of setupFilenameAndContent with no name or content");
		sXmlFileName = "CVI_" + sOriginStateCode + "_To_" + sDestinationStateCode + "_" + sCVINo + ".cvi";
		sAttachmentFileName = FileUtils.replaceInvalidFileNameChars(sAttachmentFileName);
		sXmlFileName = FileUtils.replaceInvalidFileNameChars(sXmlFileName);		
	}
	
	private String buildXml() {
		StdeCviXmlBuilder xmlBuilder = new StdeCviXmlBuilder(stdXml);
		VetLookup vet = new VetLookup( iIssuedByKey );
		xmlBuilder.setCviNumber(sCVINo);
		xmlBuilder.setIssueDate(dDateIssued);
		if( !bXFA ) {  // Don't override vet that signed XFA document
			Element eVet = null;
			if( bImport ) {
				xmlBuilder.setVet(sIssuedByName);
			}
			else {
				String sVetName = vet.getLastName() + ", " + vet.getFirstName();
				eVet = xmlBuilder.setVet(sVetName, vet.getLicenseNo(), vet.getNAN(), vet.getPhoneDigits());
				xmlBuilder.setAddress(eVet, vet.getAddress(), vet.getCity(), vet.getState(), vet.getZipCode());
			}
		} // End if !bXFA
		// Expiration date will be set automatically from getXML();
		xmlBuilder.setPurpose(sStdPurpose);
		// We don't enter the person name, normally  or add logic to tell prem name from person name.
		Element eOrigin = xmlBuilder.setOrigin(sOriginPIN, sOriginName, null, sOriginPhone);
		xmlBuilder.setAddress(eOrigin, sOriginAddress, sOriginCity, sOriginStateCode, sOriginZipCode);
		Element eDestination = xmlBuilder.setDestination(sDestinationPIN, sDestinationName, null, sDestinationPhone);
		xmlBuilder.setAddress(eDestination, sDestinationAddress, sDestinationCity, sDestinationStateCode, sDestinationZipCode);
		if( !bXFA ) { // CO/KS doesn't do animals and groups the way we do.  Don't override.
			// Add animals and groups.  This logic is tortured!
			// This could be greatly improved to better coordinate with CO/KS list of animals and the standard's group concept.
			for( SpeciesRecord sr : aSpecies ) {
				// Only add group lot if not officially IDd so count ids and subtract
				String sSpeciesCode = sr.sSpeciesCode;
				int iCountIds = 0;
				if( aAnimalIDs != null ) {
					for( AnimalIDRecord ar : aAnimalIDs ) {
						if( ar.sSpeciesCode.equals(sSpeciesCode) ) {
							iCountIds++;
						}
					}
				}
				if( iCountIds < sr.iNumber ) {
					xmlBuilder.addGroup(sr.iNumber - iCountIds, "Group Lot", sSpeciesCode, null, null );
				}
			}
			if( aAnimalIDs != null ) {
				for( AnimalIDRecord ar : aAnimalIDs ) {
					String sType = IDTypeGuesser.getTagType(ar.sTag);
					xmlBuilder.addAnimal( ar.sSpeciesCode, dDateIssued,null,null,null,sType,ar.sTag);
				}
			}
			// Also, don't check size on XFA PDFs because we don't control those.
			if( bAttachmentFileBytes != null ) {
				if( bAttachmentFileBytes.length > MAX_SANE_SIZE ) {
					MessageDialog.messageWait(dlg, "Civet Warning", "The PDF attachment is larger than normal.\nCheck your scanner settings");
				}
			}
		} // End if !bXFA
		CviMetaDataXml metaData = new CviMetaDataXml();
		metaData.setCertificateNbr(sCVINo);
		metaData.setBureauReceiptDate(dDateReceived);
		if( aErrorKeys != null )
			for( String sErr : aErrorKeys ) {
				if( sErr != null )
				metaData.addError(sErr);
			}
		if( sErrorNotes != null && sErrorNotes.trim().length() > 0 )
			metaData.setErrorNote(sErrorNotes);
		metaData.setCVINumberSource(sCVINbrSource);
	System.out.println(metaData.getXmlString());
		xmlBuilder.addMetadataAttachement(metaData);
		xmlBuilder.addPDFAttachement(bAttachmentFileBytes, sAttachmentFileName);
		return xmlBuilder.getXMLString();
	}
	
	private void saveXml(String sStdXml) {
		final String sFilePath = CivetConfig.getToFileDirPath() + sXmlFileName;
		final File fileOut = new File(sFilePath);
		try {
			PrintWriter pw = new PrintWriter( new FileOutputStream( fileOut ) );
			pw.print(sStdXml);
			pw.flush();
			pw.close();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					dlg.getController().setLastSavedFile(fileOut);
					CivetEditDialog dlgParent = dlg.getDialogParent();
					if( dlgParent != null ) {
						dlgParent.getController().setLastSavedFile(fileOut);
					}
				}
			});
		} catch (final FileNotFoundException e) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					logger.error("Could not save " + sFilePath, e);
					MessageDialog.showMessage(dlg, "Civet Error: File Save", "Could not save file\n " + sFilePath );
					prog.setVisible(false);
					prog.dispose();
				}
			});
			bCancel = true;
			return;
		}
	}
	
	private void saveEmail(String sStdXml) {
		String sFilePath = null;
		if( !bImport ) 
			sFilePath = CivetConfig.getEmailOutDirPath() + sXmlFileName;
		else if( aErrorKeys != null && aErrorKeys.size() > 0 )
			sFilePath = CivetConfig.getEmailErrorsDirPath() + sXmlFileName;
		else 
			return;
		File fileOut = new File(sFilePath);
		try {
			PrintWriter pw = new PrintWriter( new FileOutputStream( fileOut ) );
			pw.print(sStdXml);
			pw.flush();
			pw.close();
		} catch (IOException e) {
			logger.error("Could not save " + sFilePath, e);
			MessageDialog.messageLater(dlg, "Civet Error: File Save", "Could not save file\n " + sFilePath );
			return;
		} 
	}

	public void setNoEmail() {
		bNoEmail = true;
	}

} // End class SaveCVIThread
