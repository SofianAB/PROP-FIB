/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package presentacio;

import java.io.IOException;

/**
 *
 * @author Usuari
 */
public class prova {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater (
        new Runnable() {
        public void run() {
            ControladorPresentacio ctrlPresentacion = null;
            try {
                ctrlPresentacion = new ControladorPresentacio();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ctrlPresentacion.inicialitzarPresentacio();
        }
        });
    }    

}

