package com.utn.tp_spring_grupal.afip;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utilidades XML para las respuestas de ARCA: parseo seguro con namespaces y decodificación de caracteres. */
final class XmlUtil {

    private static final Pattern CHARSET_HEADER = Pattern.compile("charset\\s*=\\s*\"?([\\w.:-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHARSET_XML = Pattern.compile("^\\s*<\\?xml[^>]*encoding\\s*=\\s*[\"']([\\w.:-]+)[\"']");

    private XmlUtil() {
    }

    /** Parser DOM con namespaces, sin DTD ni entidades externas (XXE). */
    static Document parsear(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new AfipComunicacionException(
                    "ARCA devolvió una respuesta que no se pudo interpretar. Intentá de nuevo en unos minutos.", e);
        }
    }

    /** Texto del primer descendiente con ese nombre local (sin importar prefijo o namespace); null si no hay. */
    static String texto(Element desde, String nombreLocal) {
        Element e = primero(desde, nombreLocal);
        if (e == null) {
            return null;
        }
        String t = e.getTextContent();
        return t == null || t.isBlank() ? null : t.trim();
    }

    static Element primero(Element desde, String nombreLocal) {
        NodeList nodos = desde.getElementsByTagNameNS("*", nombreLocal);
        return nodos.getLength() == 0 ? null : (Element) nodos.item(0);
    }

    static java.util.List<Element> todos(Element desde, String nombreLocal) {
        NodeList nodos = desde.getElementsByTagNameNS("*", nombreLocal);
        java.util.List<Element> out = new java.util.ArrayList<>();
        for (int i = 0; i < nodos.getLength(); i++) {
            Node n = nodos.item(i);
            out.add((Element) n);
        }
        return out;
    }

    /**
     * Decodifica el cuerpo: primero el charset del Content-Type, si no el de la declaración XML
     * (ARCA responde a veces en ISO-8859-1) y por último UTF-8.
     */
    static String decodificar(byte[] bytes, String contentType) {
        Charset cs = null;
        if (contentType != null) {
            Matcher m = CHARSET_HEADER.matcher(contentType);
            if (m.find()) {
                cs = charset(m.group(1));
            }
        }
        if (cs == null) {
            String inicio = new String(bytes, 0, Math.min(bytes.length, 200), StandardCharsets.ISO_8859_1);
            Matcher m = CHARSET_XML.matcher(inicio);
            if (m.find()) {
                cs = charset(m.group(1));
            }
        }
        return new String(bytes, cs != null ? cs : StandardCharsets.UTF_8);
    }

    private static Charset charset(String nombre) {
        try {
            return Charset.forName(nombre);
        } catch (Exception e) {
            return null;
        }
    }

    /** Escapa un valor de texto para incluirlo dentro de un elemento XML. */
    static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
