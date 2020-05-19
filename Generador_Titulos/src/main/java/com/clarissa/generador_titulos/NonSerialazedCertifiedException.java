package com.clarissa.generador_titulos;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Cynthia Clarissa
 */
public class NonSerialazedCertifiedException extends Exception {
    public NonSerialazedCertifiedException() {
        super("Este perro no tiene número de serie en su Certificado!!!!");
    }
}
