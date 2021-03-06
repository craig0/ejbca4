/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
 
package org.ejbca.core.protocol.ws.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509Name;
import org.ejbca.ui.cli.ErrorAdminCommandException;
import org.ejbca.ui.cli.IAdminCommand;
import org.ejbca.ui.cli.IllegalAdminCommandException;
import org.ejbca.util.keystore.KeyTools;

import com.novosec.pkix.asn1.cmp.PKIBody;
import com.novosec.pkix.asn1.cmp.PKIHeader;
import com.novosec.pkix.asn1.cmp.PKIMessage;
import com.novosec.pkix.asn1.crmf.CertRequest;





/**
 * Sends a CMP certificate request message signed by an unknown RA (ra certificate not stored) and receives an error message.
 *
 * @version $Id: NestedCrmfRequestMissingStoredCertTestCommand.java 15009 2012-06-18 12:49:30Z primelars $
 */
public class NestedCrmfRequestMissingStoredCertTestCommand extends CMPNestedMessageTestBaseCommand implements IAdminCommand{
	

	
	private static final int ARG_HOSTNAME    		= 1;
	private static final int ARG_CAFILE      		= 2;
	private static final int ARG_KEYSTOREPATH		= 3;
	private static final int ARG_KEYSTOREPASSWORD	= 4;
	private static final int ARG_CERTNAMEINKEYSTORE = 5;
	private static final int ARG_PORT        		= 6;
	private static final int ARG_URLPATH     		= 7;

	private static final int NR_OF_MANDATORY_ARGS = ARG_CERTNAMEINKEYSTORE+1;
	private static final int MAX_NR_OF_ARGS = ARG_URLPATH+1;
	
    PrivateKey innerSignKey;
    Certificate innerCertificate;
    int reqId;

    /**
     * Creates a new instance of RaAddUserCommand
     *
     * @param args command line arguments
     */
    public NestedCrmfRequestMissingStoredCertTestCommand(String[] args) {
    	super();
    	
    	if(args.length < NR_OF_MANDATORY_ARGS || args.length > MAX_NR_OF_ARGS){
        	usage();
        	System.exit(-1); // NOPMD, this is not a JEE app
        }
    	
    	hostname = args[ARG_HOSTNAME];
    	String certFile = args[ARG_CAFILE];
        port = args.length>ARG_PORT ? Integer.parseInt(args[ARG_PORT].trim()):8080;
        urlPath = args.length>ARG_URLPATH && args[ARG_URLPATH].toLowerCase().indexOf("null")<0 ? args[ARG_URLPATH].trim():null;

        try {
			cacert = (X509Certificate)this.certificateFactory.generateCertificate(new FileInputStream(certFile));
			final KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
			keygen.initialize(2048);
			popokeys = keygen.generateKeyPair();
		} catch (CertificateException e3) {
			e3.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (FileNotFoundException e3) {
			e3.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		}

        init(args);

    }
    private void init(String[] args) {
    	
        FileInputStream file_inputstream;
		try {
			String pwd = args[ARG_KEYSTOREPASSWORD];
			String certNameInKeystore = args[ARG_CERTNAMEINKEYSTORE];
			file_inputstream = new FileInputStream(args[ARG_KEYSTOREPATH]);
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(file_inputstream, pwd.toCharArray());
			System.out.println("Keystore size " + keyStore.size());
			Enumeration aliases = keyStore.aliases();
			while(aliases.hasMoreElements()) {
				System.out.println(aliases.nextElement());
			}
			Key key=keyStore.getKey(certNameInKeystore, pwd.toCharArray());
			getPrintStream().println("Key information " + key.getAlgorithm() + " " + key.getFormat());
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key.getEncoded());
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			innerSignKey = keyFactory.generatePrivate(keySpec);
			innerCertificate = keyStore.getCertificate(certNameInKeystore);
		} catch (FileNotFoundException e2) {
			e2.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (KeyStoreException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (CertificateException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (InvalidKeySpecException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		}
              
    }

    
    /**
     * Runs the command
     *
     * @throws IllegalAdminCommandException Error in command args
     * @throws ErrorAdminCommandException Error running command
     */
    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {
		
    	try {
    	
    		CertRequest certReq = genCertReq(userDN, null);

    		PKIMessage certMsg = genPKIMessage(false, certReq);
    		if ( certMsg==null ) {
    			getPrintStream().println("No certificate request.");
    			System.exit(-1);
    		}
    		AlgorithmIdentifier pAlg = new AlgorithmIdentifier(PKCSObjectIdentifiers.sha1WithRSAEncryption);
    		certMsg.getHeader().setProtectionAlg(pAlg);
    		certMsg.getHeader().setSenderKID(new DEROctetString("EMPTY".getBytes()));
    		PKIMessage signedMsg = signPKIMessage(certMsg, innerSignKey);
    		addExtraCert(signedMsg, innerCertificate);
    		if ( signedMsg==null ) {
    			getPrintStream().println("No protected message.");
    			System.exit(-1);
    		}
        
        
    		PKIHeader myPKIHeader = new PKIHeader(new DERInteger(2), new GeneralName(new X509Name("CN=CMSSender,C=SE")), new GeneralName(new X509Name(((X509Certificate)cacert).getSubjectDN()
    				.getName())));
    		myPKIHeader.setMessageTime(new DERGeneralizedTime(new Date()));
    		// senderNonce
    		myPKIHeader.setSenderNonce(new DEROctetString(nonce));
    		// TransactionId
    		myPKIHeader.setTransactionID(new DEROctetString(nonce));
    		//myPKIHeader.addGeneralInfo(new InfoTypeAndValue(ASN1Sequence.getInstance(crmfMsg)));
    		byte[] recipNonce = new byte[16];
    		random.nextBytes(recipNonce);
    		myPKIHeader.setRecipNonce(new DEROctetString(recipNonce));

    		PKIBody myPKIBody = new PKIBody(signedMsg, 20); // NestedMessageContent
    		PKIMessage myPKIMessage = new PKIMessage(myPKIHeader, myPKIBody);
    		KeyPair signkeys = KeyTools.genKeys("1024", "RSA");
    		PKIMessage cmsMessage = signPKIMessage(myPKIMessage, signkeys.getPrivate());
        
        
        
    		reqId = signedMsg.getBody().getIr().getCertReqMsg(0).getCertReq().getCertReqId().getValue().intValue();
    		final ByteArrayOutputStream bao = new ByteArrayOutputStream();
    		final DEROutputStream out = new DEROutputStream(bao);
    		out.writeObject(cmsMessage);
    		final byte[] ba = bao.toByteArray();
    		// Send request and receive response
    		final byte[] resp = sendCmp(ba);
    		if ( resp==null || resp.length <= 0 ) {
    			getPrintStream().println("No response message.");
    			System.exit(-1);
    		}
        
    		PKIMessage respObject = PKIMessage.getInstance(new ASN1InputStream(new ByteArrayInputStream(resp)).readObject());
    		if(respObject == null) {
    			getPrintStream().println("No response message object could be optained");
    			System.exit(-1);
    		}
    		
    		PKIBody body = respObject.getBody();
    		if(body.getTagNo() != 23) {
    			getPrintStream().println("Expected tagnr 23, but found " + body.getTagNo());
    			System.exit(-1);
    		}
			getPrintStream().println("Response tagnr checked 23 ok");
			getPrintStream().println("FailInfo error code: " + body.getError().getPKIStatus().getFailInfo().getPadBits());
			getPrintStream().println("Error Message: " + body.getError().getPKIStatus().getStatusString().getString(0).getString());
			
		} catch (IOException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (InvalidKeyException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (SignatureException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (NoSuchProviderException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (CertificateEncodingException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		} catch (Exception e) {
			e.printStackTrace(getPrintStream());
			System.exit(-1);
		}
		
		getPrintStream().println("Test successfull");
    }   	
 
	protected void usage() {
		getPrintStream().println("Command used to send a cmp certificate request inside a NestedMessageContent signed by an untrusted RA.");
		getPrintStream().println("Usage : missingstoredcert <hostname> <CA certificate file name> <CMS keystore (p12)> <keystore password> <CMS certificate in keystore> [<port>] [<URL path of servlet. use 'null' to get EJBCA (not proxy) default>]");
		getPrintStream().println("EJBCA build configuration requirements: cmp.operationmode=ra, cmp.authenticationmodule=EndEntityCertificate, cmp.authenticationparameters=AdminCA1, cmp.racertificatepath=/opt/racerts");

	}
	
}
