package de.sebastianbrunnert.bringmeistervoice.bean;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Objekt, das den Warenkorb darstellt.
 *
 * @author Sebastian Brunnert
 */
public class Cart {

    @Getter @Setter
    private List<CartProduct> products;
    @Getter @Setter
    private List<Total> totals;


    /**
     * Im Aufbau des Backends werden verschiedene Typen von Kosten angegeben. Diese werden hier repräsentiert.
     *
     * @author Sebastian Brunnert
     */
    public static class Total {

        @Getter
        private double value;

    }

    /**
     * Objekt, das Produkte im Warenkorb darstellt.
     *
     * @author Sebastian Brunnert
     */
    public static class CartProduct {

        @Getter
        private String name;
        @Getter
        private String quantity;
        @Getter
        private String packing;
        @Getter
        private String sku;

    }

}
