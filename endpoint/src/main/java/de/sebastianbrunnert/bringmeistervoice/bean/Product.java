package de.sebastianbrunnert.bringmeistervoice.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Objekt, das die Daten der Produktsuche verwaltet.
 *
 * @author Sebastian Brunnert
 */
@NoArgsConstructor
public class Product {

    @Getter @Setter
    private String name;
    @Getter @Setter
    private String sku;
    @Getter @Setter
    private int unitId;
    @Getter @Setter @JsonIgnore
    private String packing;

}
