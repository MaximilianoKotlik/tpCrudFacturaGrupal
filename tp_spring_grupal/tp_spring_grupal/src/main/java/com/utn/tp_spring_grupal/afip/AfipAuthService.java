package com.utn.tp_spring_grupal.afip;

import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;

/**
 * Autenticación contra WSAA. El ticket (token + sign) se guarda solo en memoria hasta su vencimiento real
 * (ARCA lo entrega por ~12 h y no permite pedir otro mientras siga vigente), con acceso sincronizado.
 */
@Service
public class AfipAuthService {

    private static final Logger log = LoggerFactory.getLogger(AfipAuthService.class);

    @Value("${afip.keystore.path}")
    private String keystorePath;

    @Value("${afip.keystore.password}")
    private String keystorePassword;

    @Value("${afip.wsaa.url}")
    private String wsaaUrl;

    @Value("${afip.timeout.connect-ms:10000}")
    private int connectTimeoutMs = 10_000;

    @Value("${afip.timeout.read-ms:30000}")
    private int readTimeoutMs = 30_000;

    private LoginTicket ticket;

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /** Token y Sign vigentes; si no hay o están por vencer solicita un ticket nuevo. */
    public synchronized String[] obtenerTokenYSign() {
        if (ticket == null || !ticket.vigente(ZonedDateTime.now())) {
            ticket = solicitarTicket();
            log.info("Ticket de acceso WSAA obtenido, vence {}", ticket.vencimiento());
        }
        return new String[]{ticket.token(), ticket.sign()};
    }

    private LoginTicket solicitarTicket() {
        try {
            String respuesta;
            try {
                respuesta = new SoapClient(wsaaUrl, connectTimeoutMs, readTimeoutMs)
                        .post("", envoltorio(firmarTra()));
            } catch (AfipComunicacionException e) {
                // WSAA informa sus errores (p. ej. ya posee un TA válido) como SOAP Fault con HTTP 500.
                if (e.getCuerpo() != null) {
                    return LoginTicket.parsear(e.getCuerpo());
                }
                throw e;
            }
            return LoginTicket.parsear(respuesta);
        } catch (AfipAutenticacionException | AfipComunicacionException e) {
            throw e;
        } catch (Exception e) {
            log.error("No se pudo firmar el pedido de acceso a ARCA", e);
            throw new AfipAutenticacionException("No se pudo autenticar con ARCA: revisá que el certificado "
                    + "digital y su clave estén bien configurados.", e);
        }
    }

    /** Arma el TRA y lo firma (CMS con BouncyCastle) con el certificado del .pfx. */
    private String firmarTra() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        // getInputStream (no getFile) para que también funcione empaquetado en un jar.
        try (InputStream in = new DefaultResourceLoader().getResource(keystorePath).getInputStream()) {
            ks.load(in, keystorePassword.toCharArray());
        }
        String alias = ks.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, keystorePassword.toCharArray());
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

        ZonedDateTime now = ZonedDateTime.now();
        String traXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<loginTicketRequest version=\"1.0\"><header>"
                + "<uniqueId>" + (System.currentTimeMillis() / 1000) + "</uniqueId>"
                + "<generationTime>" + now.minusMinutes(5).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "</generationTime>"
                + "<expirationTime>" + now.plusMinutes(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "</expirationTime>"
                + "</header><service>wsfe</service></loginTicketRequest>";

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(privateKey);
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder().setProvider("BC").build()).build(signer, cert));
        gen.addCertificates(new JcaCertStore(Collections.singletonList(cert)));
        CMSSignedData signed = gen.generate(new CMSProcessableByteArray(traXml.getBytes(StandardCharsets.UTF_8)), true);
        return Base64.getEncoder().encodeToString(signed.getEncoded());
    }

    private static String envoltorio(String cmsBase64) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:nso=\"http://wsaa.afip.gov.ar/ws/services/LoginCms\">"
                + "<soapenv:Header/><soapenv:Body><nso:loginCms><nso:in0>" + cmsBase64
                + "</nso:in0></nso:loginCms></soapenv:Body></soapenv:Envelope>";
    }
}
