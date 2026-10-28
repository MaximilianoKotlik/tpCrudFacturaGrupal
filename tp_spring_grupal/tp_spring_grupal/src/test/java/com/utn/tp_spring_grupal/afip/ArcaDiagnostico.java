package com.utn.tp_spring_grupal.afip;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Herramienta opt-in de diagnostico (SOLO LECTURA) contra ARCA. No es un test: se ejecuta con
 * {@code gradlew.bat arcaDiagnostico}. No arranca Spring, Postgres ni DataLoader.
 * Nunca imprime token, sign, clave del keystore ni el certificado.
 */
public class ArcaDiagnostico {

    private static final String NS = "http://ar.gov.afip.dif.FEV1/";
    private static String cuit;
    private static String wsfeUrl;
    private static String token;
    private static String sign;

    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream("src/main/resources/application.properties")) {
            p.load(in);
        }
        cuit = p.getProperty("afip.cuit");
        wsfeUrl = p.getProperty("afip.wsfe.url");
        System.out.println("CUIT=" + cuit + " WSAA=" + p.getProperty("afip.wsaa.url") + " WSFE=" + wsfeUrl);

        AfipAuthService auth = new AfipAuthService();
        set(auth, "keystorePath", p.getProperty("afip.keystore.path"));
        set(auth, "keystorePassword", p.getProperty("afip.keystore.password"));
        set(auth, "wsaaUrl", p.getProperty("afip.wsaa.url"));
        try {
            String[] ts = auth.obtenerTokenYSign();
            token = ts[0];
            sign = ts[1];
            System.out.println("[WSAA] OK: token/sign obtenidos (no se muestran)");
        } catch (Exception e) {
            System.out.println("[WSAA] FALLO: " + limpiar(e.getMessage()));
            return;
        }

        System.out.println("\n== FEParamGetPtosVenta ==");
        String r = call("FEParamGetPtosVenta", "");
        for (String b : bloques(r, "PtoVenta")) {
            System.out.println("  Nro=" + tag(b, "Nro") + " EmisionTipo=" + tag(b, "EmisionTipo")
                    + " Bloqueado=" + tag(b, "Bloqueado") + " FchBaja=" + tag(b, "FchBaja"));
        }
        resumenErrores(r);

        System.out.println("\n== FEParamGetTiposMonedas ==");
        r = call("FEParamGetTiposMonedas", "");
        for (String b : bloques(r, "Moneda")) {
            System.out.println("  Id=" + tag(b, "Id") + " Desc=" + tag(b, "Desc") + " FchDesde=" + tag(b, "FchDesde")
                    + " FchHasta=" + tag(b, "FchHasta"));
        }
        resumenErrores(r);

        System.out.println("\n== FEParamGetTiposCbte ==");
        r = call("FEParamGetTiposCbte", "");
        for (String b : bloques(r, "CbteTipo")) {
            System.out.println("  Id=" + tag(b, "Id") + " Desc=" + tag(b, "Desc"));
        }
        resumenErrores(r);

        System.out.println("\n== FEParamGetCotizacion ==");
        for (String mon : new String[]{"DOL", "060"}) {
            r = call("FEParamGetCotizacion", "<ar:MonId>" + mon + "</ar:MonId>");
            System.out.println("  " + mon + ": MonId=" + tag(r, "MonId") + " MonCotiz=" + tag(r, "MonCotiz")
                    + " FchCotiz=" + tag(r, "FchCotiz") + errores(r));
        }

        for (String[] par : new String[][]{{"FEParamGetTiposDoc", "DocTipo"}, {"FEParamGetTiposIva", "IvaTipo"},
                {"FEParamGetTiposConcepto", "ConceptoTipo"}}) {
            System.out.println("\n== " + par[0] + " ==");
            r = call(par[0], "");
            for (String b : bloques(r, par[1])) {
                System.out.println("  Id=" + tag(b, "Id") + " Desc=" + tag(b, "Desc"));
            }
            resumenErrores(r);
        }

        for (String clase : new String[]{"A", "B", "C"}) {
            System.out.println("\n== FEParamGetCondicionIvaReceptor ClaseCmp=" + clase + " ==");
            r = call("FEParamGetCondicionIvaReceptor", "<ar:ClaseCmp>" + clase + "</ar:ClaseCmp>");
            for (String b : bloques(r, "CondicionIvaReceptor")) {
                System.out.println("  Id=" + tag(b, "Id") + " Desc=" + tag(b, "Desc") + " Cmp_Clase=" + tag(b, "Cmp_Clase"));
            }
            resumenErrores(r);
        }

        System.out.println("\n== FECompUltimoAutorizado ==");
        int[] tipos = {6, 1, 11};
        for (int pv = 1; pv <= 5; pv++) {
            for (int t : tipos) {
                r = call("FECompUltimoAutorizado", "<ar:PtoVta>" + pv + "</ar:PtoVta><ar:CbteTipo>" + t + "</ar:CbteTipo>");
                String err = errores(r);
                String nro = tag(r, "CbteNro");
                System.out.println("  PtoVta=" + pv + " CbteTipo=" + t + " -> "
                        + (err.isEmpty() && nro != null ? "OK CbteNro=" + nro : "ERROR" + err
                        + (nro != null ? " (CbteNro=" + nro + ")" : "")));
            }
        }
    }

    private static String call(String op, String inner) throws Exception {
        String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ar=\"" + NS + "\">"
                + "<soapenv:Header/><soapenv:Body><ar:" + op + "><ar:Auth><ar:Token>" + token + "</ar:Token><ar:Sign>"
                + sign + "</ar:Sign><ar:Cuit>" + cuit + "</ar:Cuit></ar:Auth>" + inner + "</ar:" + op + ">"
                + "</soapenv:Body></soapenv:Envelope>";
        HttpURLConnection c = (HttpURLConnection) URI.create(wsfeUrl).toURL().openConnection();
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        c.setRequestProperty("SOAPAction", NS + op);
        c.setDoOutput(true);
        c.getOutputStream().write(soap.getBytes(StandardCharsets.UTF_8));
        int st = c.getResponseCode();
        var is = st >= 400 ? c.getErrorStream() : c.getInputStream();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        return st >= 400 ? "HTTP" + st + " " + body : body;
    }

    private static String tag(String xml, String name) {
        Matcher m = Pattern.compile("<(?:[A-Za-z0-9]+:)?" + name + ">(.*?)</(?:[A-Za-z0-9]+:)?" + name + ">", Pattern.DOTALL).matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    private static List<String> bloques(String xml, String name) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("<(?:[A-Za-z0-9]+:)?" + name + ">(.*?)</(?:[A-Za-z0-9]+:)?" + name + ">", Pattern.DOTALL).matcher(xml);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static String errores(String xml) {
        StringBuilder sb = new StringBuilder();
        Matcher m = Pattern.compile("<(?:[A-Za-z0-9]+:)?Err>(.*?)</(?:[A-Za-z0-9]+:)?Err>", Pattern.DOTALL).matcher(xml);
        while (m.find()) sb.append(" [Err ").append(tag(m.group(1), "Code")).append(": ").append(tag(m.group(1), "Msg")).append("]");
        if (xml.startsWith("HTTP")) sb.append(" [").append(limpiar(xml)).append("]");
        return sb.toString();
    }

    private static void resumenErrores(String xml) {
        String e = errores(xml);
        if (!e.isEmpty()) System.out.println("  ERRORES:" + e);
    }

    private static String limpiar(String s) {
        if (s == null) return "null";
        return s.replaceAll("<token>.*?</token>", "<token>…</token>").replaceAll("<sign>.*?</sign>", "<sign>…</sign>");
    }

    private static void set(Object o, String f, Object v) throws Exception {
        Field fl = o.getClass().getDeclaredField(f);
        fl.setAccessible(true);
        fl.set(o, v);
    }
}
