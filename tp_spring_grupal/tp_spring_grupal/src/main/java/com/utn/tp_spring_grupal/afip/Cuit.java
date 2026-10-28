package com.utn.tp_spring_grupal.afip;

/** Validación del CUIT/CUIL por su dígito verificador (módulo 11). Clase pura. */
public final class Cuit {

    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private Cuit() {
    }

    public static boolean esValido(String cuit) {
        if (cuit == null) {
            return false;
        }
        String d = cuit.replaceAll("[^0-9]", "");
        if (d.length() != 11 || d.length() != cuit.replace("-", "").trim().length()) {
            return false;
        }
        int dv = digitoVerificador(d.substring(0, 10));
        return dv >= 0 && dv == d.charAt(10) - '0';
    }

    /** Dígito verificador de los primeros 10 dígitos; -1 si el resto da 10 (no existe CUIT con ese prefijo). */
    public static int digitoVerificador(String primerosDiez) {
        int suma = 0;
        for (int i = 0; i < PESOS.length; i++) {
            suma += (primerosDiez.charAt(i) - '0') * PESOS[i];
        }
        int dv = 11 - suma % 11;
        if (dv == 11) {
            return 0;
        }
        return dv == 10 ? -1 : dv;
    }

    /**
     * CUIT válido (con guiones) para un prefijo de 2 dígitos y un número base de 8 dígitos; si para esa base no
     * existe dígito verificador se avanza al siguiente número.
     */
    public static String completar(String prefijo, int base) {
        int n = base;
        while (true) {
            String diez = prefijo + String.format("%08d", n);
            int dv = digitoVerificador(diez);
            if (dv >= 0) {
                return prefijo + "-" + String.format("%08d", n) + "-" + dv;
            }
            n++;
        }
    }
}
