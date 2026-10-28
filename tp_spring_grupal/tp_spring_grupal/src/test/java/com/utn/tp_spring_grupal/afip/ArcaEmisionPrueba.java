package com.utn.tp_spring_grupal.afip;

import org.w3c.dom.Element;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Properties;

/**
 * Herramienta opt-in contra ARCA HOMOLOGACION: consulta FEParamGetCondicionIvaReceptor y emite UNA Factura C de
 * prueba a Consumidor Final. Se ejecuta con {@code gradlew.bat arcaEmisionPrueba}. No arranca Spring ni Postgres,
 * hace un solo login a WSAA (ARCA no permite dos tickets vigentes) y nunca imprime token, sign ni claves.
 * Se niega a correr si el WSFE configurado no es de homologacion.
 */
public class ArcaEmisionPrueba {

    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream("src/main/resources/application.properties")) {
            p.load(in);
        }
        String wsfeUrl = p.getProperty("afip.wsfe.url");
        String wsaaUrl = p.getProperty("afip.wsaa.url");
        if (!wsfeUrl.contains("homo") || !wsaaUrl.contains("homo")) {
            System.out.println("ABORTADO: la URL no es de homologacion (" + wsfeUrl + "). Esta herramienta es solo para homologacion.");
            return;
        }
        int puntoVenta = Integer.parseInt(p.getProperty("afip.punto-venta", "2"));
        System.out.println("CUIT=" + p.getProperty("afip.cuit") + " WSFE=" + wsfeUrl + " ptoVta=" + puntoVenta);

        AfipAuthService auth = new AfipAuthService();
        set(auth, "keystorePath", p.getProperty("afip.keystore.path"));
        set(auth, "keystorePassword", p.getProperty("afip.keystore.password"));
        set(auth, "wsaaUrl", wsaaUrl);
        AfipWsfeService wsfe = new AfipWsfeService(auth);
        set(wsfe, "cuit", p.getProperty("afip.cuit"));
        set(wsfe, "wsfeUrl", wsfeUrl);

        try {
            auth.obtenerTokenYSign();
            System.out.println("[WSAA] OK (token/sign no se muestran)");
        } catch (RuntimeException e) {
            System.out.println("[WSAA] FALLO: " + e.getMessage());
            return;
        }

        for (String clase : new String[]{"A", "B", "C"}) {
            System.out.println("\n== FEParamGetCondicionIvaReceptor ClaseCmp=" + clase + " ==");
            String r = wsfe.invocar("FEParamGetCondicionIvaReceptor", "<ar:ClaseCmp>" + clase + "</ar:ClaseCmp>");
            Element raiz = XmlUtil.parsear(r).getDocumentElement();
            for (Element c : XmlUtil.todos(raiz, "CondicionIvaReceptor")) {
                System.out.println("  Id=" + XmlUtil.texto(c, "Id") + " Desc=" + XmlUtil.texto(c, "Desc"));
            }
            WsfeRespuesta w = WsfeRespuesta.parsear(r);
            if (!w.errores().isEmpty()) {
                System.out.println("  ERRORES: " + w.errores());
            }
        }

        System.out.println("\n== Emision de prueba: Factura C, Consumidor Final, PES ==");
        int tipoFacturaC = 11;
        long ultimo = wsfe.obtenerUltimoComprobante(puntoVenta, tipoFacturaC);
        System.out.println("Ultimo autorizado (ptoVta " + puntoVenta + ", tipo 11): " + ultimo);
        SolicitudCae solicitud = new SolicitudCae(puntoVenta, tipoFacturaC, ultimo + 1,
                LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")), new BigDecimal("100.00"),
                new BigDecimal("100.00"), new BigDecimal("0.00"), DocumentoReceptor.CONSUMIDOR_FINAL, "PES",
                BigDecimal.ONE, 5, List.of());
        WsfeRespuesta resp = wsfe.solicitarCae(solicitud);
        System.out.println("Resultado=" + resp.resultado() + " aprobada=" + resp.aprobada());
        System.out.println("CAE=" + resp.cae() + " Vto=" + resp.caeFchVto());
        resp.errores().forEach(m -> System.out.println("  Error " + m));
        resp.observaciones().forEach(m -> System.out.println("  Obs   " + m));
        if (!resp.aprobada()) {
            System.out.println("Motivo:\n" + resp.motivoRechazo());
        } else {
            System.out.println("Consulta de control: "
                    + wsfe.consultarComprobante(puntoVenta, tipoFacturaC, ultimo + 1).map(c -> "CAE " + c.cae()));
        }
    }

    private static void set(Object o, String f, Object v) throws Exception {
        Field fl = o.getClass().getDeclaredField(f);
        fl.setAccessible(true);
        fl.set(o, v);
    }
}
