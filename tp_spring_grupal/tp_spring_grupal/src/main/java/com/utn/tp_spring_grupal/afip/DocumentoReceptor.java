package com.utn.tp_spring_grupal.afip;

/**
 * Documento del receptor tal como lo pide ARCA (DocTipo / DocNro). Clase pura.
 * Códigos verificados con FEParamGetTiposDoc: 80 = CUIT, 96 = DNI, 99 = Doc. (Otro) / Consumidor Final.
 */
public record DocumentoReceptor(int tipo, long numero) {

    public static final int TIPO_CUIT = 80;
    public static final int TIPO_DNI = 96;
    public static final int TIPO_CONSUMIDOR_FINAL = 99;

    public static final DocumentoReceptor CONSUMIDOR_FINAL = new DocumentoReceptor(TIPO_CONSUMIDOR_FINAL, 0);

    /**
     * Infiere el tipo por la cantidad de dígitos (el cliente guarda un único campo): 11 = CUIT/CUIL,
     * 7 u 8 = DNI. Sin documento es Consumidor Final. Cualquier otra longitud es un dato inválido.
     */
    public static DocumentoReceptor deCuitCuil(String valor) {
        String digitos = valor == null ? "" : valor.replaceAll("[^0-9]", "");
        if (digitos.isEmpty()) {
            return CONSUMIDOR_FINAL;
        }
        if (digitos.length() == 11) {
            return new DocumentoReceptor(TIPO_CUIT, Long.parseLong(digitos));
        }
        if (digitos.length() == 7 || digitos.length() == 8) {
            return new DocumentoReceptor(TIPO_DNI, Long.parseLong(digitos));
        }
        throw new IllegalArgumentException("El documento del cliente debe ser un CUIT/CUIL (11 dígitos) o un DNI "
                + "(7 u 8 dígitos); tiene " + digitos.length() + " dígitos.");
    }
}
