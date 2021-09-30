package de.sebastianbrunnert.bringmeistervoice.bean;

import lombok.Getter;
import lombok.Setter;

/**
 * Objekt, das die Daten der Anmeldung verwaltet.
 *
 * @author Sebastian Brunnert
 */
public class User {

    @Getter @Setter
    private String name;
    @Getter @Setter
    private String password;

}
