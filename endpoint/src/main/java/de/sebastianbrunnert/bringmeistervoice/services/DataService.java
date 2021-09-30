package de.sebastianbrunnert.bringmeistervoice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.sebastianbrunnert.bringmeistervoice.BringmeisterVoiceMaster;
import de.sebastianbrunnert.bringmeistervoice.bean.Product;
import de.sebastianbrunnert.bringmeistervoice.bean.TimeSlot;
import de.sebastianbrunnert.bringmeistervoice.exceptions.NoProductException;
import de.sebastianbrunnert.bringmeistervoice.exceptions.TimeSlotFailureException;
import graphql.kickstart.spring.webclient.boot.GraphQLRequest;
import graphql.kickstart.spring.webclient.boot.GraphQLResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;

/**
 * Dieser Service dient zur Kommunikation mit öffentlichen und der mehr oder weniger statischen REST APIs wie
 * OpenWeatherMap oder dem Teil des GraphQL-Servers, der keiner Autorisierung bedarf. So werden hier beispielsweise
 * Produkte gesucht oder aktuelle Angebote werden geladen.
 *
 * @author Sebastian Brunnert
 */
@Service
public class DataService {

    /**
     * Instanz des Rest-Clients, der für die Kommunikation mit dem Backend verwendet wird
     */
    private RestTemplate restTemplate;

    /**
     * @param restTemplateBuilder Um Rest-Client zu erzeugen
     */
    public DataService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(30)).build();
    }

    /**
     * Instanz der Hauptklasse
     */
    @Autowired
    private BringmeisterVoiceMaster bringmeisterVoiceMaster;

    /**
     * Diese Methode stellt eine Backend Anfrage und sucht nach der angegebenen Query.
     *
     * @param query Suchanfrage
     * @param zipCode Postleitzahl des Nutzers
     * @return Das gefundene Produkt mit zugehöriger Unit ID
     * @throws NoProductException
     */
    @SneakyThrows
    public Product getFirstProduct(String query, String zipCode) throws NoProductException {
        HashMap<String,Object> variables = new HashMap<>();
        variables.put("pageSize",1);
        variables.put("query",query);
        variables.put("zipcode",zipCode);
        GraphQLRequest request = GraphQLRequest.builder()
                .query("query Search( $query: String! $pageSize: Int! $zipcode: String! ) { products( query: $query first: $pageSize zipcode: $zipcode ) { edges { node { name packing sku units { unitId } } } } }")
                .variables(variables)
                .build();

        GraphQLResponse response = BringmeisterVoiceMaster.getInstance().getUserService().getGraphQLWebClient().post(request).block();

        ObjectNode products = response.get("products", ObjectNode.class);

        if(products.get("edges").size() == 0) {
            throw new NoProductException();
        }

        ObjectNode node = (ObjectNode) products.get("edges").get(0).get("node");

        Product product = new Product();
        product.setSku(node.get("sku").textValue());
        product.setPacking(node.get("packing").textValue());
        product.setName(node.get("name").textValue());
        product.setUnitId(node.get("units").get(0).get("unitId").intValue());

        return product;
    }

    /**
     * Diese Methode sucht im Backend nach aktuellen Angeboten aus dem Liefergebiet des Nutzers. Daraus wird ein
     * String gebaut, der später auf dem Display des Alexas wiedergegeben wird.
     *
     * @param zipCode Postleitzahl des Nutzers, wo nach Angebot gesucht werden soll
     * @return Aktuelles Angebot formatiert
     */
    public String getDeal(String zipCode) {
        HashMap<String,Object> variables = new HashMap<>();
        variables.put("zipCode",zipCode);

        GraphQLRequest request = GraphQLRequest.builder()
                .query("query DealsPage ( $zipCode: String! ) { dealsPage(zipCode: $zipCode) { weeklyStars { title products(first: 1) { edges { node { name } } } } } }")
                .variables(variables)
                .build();

        GraphQLResponse response = BringmeisterVoiceMaster.getInstance().getUserService().getGraphQLWebClient().post(request).block();

        ObjectNode weeklyStars = (ObjectNode) response.get("dealsPage", ObjectNode.class).get("weeklyStars");
        return weeklyStars.get("title").textValue() + ": " + weeklyStars.get("products").get("edges").get(0).get("node").get("name").textValue();
    }

    /**
     * Diese Methode ruft OpenWeatherMap auf und prüft, ob das Wetter gut ist. Das ist dazu nötig, um später für die
     * Alexa Kommunikation zu sagen: "Es ist gerade gutes Wetter, du kannst auch selber einkaufen gehen."
     *
     * @param city Ort, wo geprüft werden soll, ob das Wetter gut ist
     * @return Boolean, ob das Wetter gut ist
     */
    public boolean isGoodWeather(String city) {
        ResponseEntity<ObjectNode> response = this.restTemplate.exchange("https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + bringmeisterVoiceMaster.getOpenWeatherMapKey(), HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), ObjectNode.class);

        if(response.getStatusCode() == HttpStatus.OK) {
            String weatherCode = response.getBody().get("weather").get(0).get("id").numberValue().toString();
            if(weatherCode.startsWith("8")) {
                return true;
            }
        }

        return false;
    }

}
