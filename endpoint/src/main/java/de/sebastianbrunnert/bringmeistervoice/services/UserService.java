package de.sebastianbrunnert.bringmeistervoice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.sebastianbrunnert.bringmeistervoice.BringmeisterVoiceMaster;
import de.sebastianbrunnert.bringmeistervoice.bean.Cart;
import de.sebastianbrunnert.bringmeistervoice.bean.Product;
import de.sebastianbrunnert.bringmeistervoice.bean.TimeSlot;
import de.sebastianbrunnert.bringmeistervoice.bean.User;
import de.sebastianbrunnert.bringmeistervoice.exceptions.AddCartItemFailureException;
import de.sebastianbrunnert.bringmeistervoice.exceptions.NoProductException;
import de.sebastianbrunnert.bringmeistervoice.exceptions.TimeSlotFailureException;
import de.sebastianbrunnert.bringmeistervoice.exceptions.WrongCredentialsException;
import graphql.kickstart.spring.webclient.boot.GraphQLRequest;
import graphql.kickstart.spring.webclient.boot.GraphQLResponse;
import graphql.kickstart.spring.webclient.boot.GraphQLWebClient;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import javax.crypto.Cipher;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Dieser durch Spring Boot automatisch erzeugte Service dient ausschließlich zur Kommunikation
 * über GraphQL mit den Bringmeister-Servern. Hier werden Benutzerkonto-bezogene Aktionen durchgeführt
 * und verwaltet wie das Anmelden selbst oder das Hinzufügen und Löschen von Produkten im Warenkorb.
 *
 * @author Sebastian Brunnert
 */
@Service
public class UserService {

    /**
     * Instanz der Hauptklasse
     */
    @Autowired
    private BringmeisterVoiceMaster bringmeisterVoiceMaster;

    /**
     * Instanz des GraphQL-Clients, der für die Kommunikation mit dem Backend verwendet wird
     */
    @Getter
    private GraphQLWebClient graphQLWebClient;

    /**
     * @param graphQLWebClient Client, der für die Kommunikation mit dem Backend verwendet wird
     */
    public UserService(GraphQLWebClient graphQLWebClient) {
        this.graphQLWebClient = graphQLWebClient;
    }

    /**
     * Diese Methode fragt im Backend eine Authenfizierung an.
     *
     * @param user Instanz des Users (bestehend aus Name und Passwort)
     * @return Token, der vom Backend zurückgegeben wird
     * @throws WrongCredentialsException
     */
    public String getToken(User user) throws WrongCredentialsException {
        HashMap<String,String> variables = new HashMap<>();
        variables.put("username", user.getName());
        variables.put("password", user.getPassword());
        GraphQLRequest request = GraphQLRequest.builder()
                .query("mutation( $username: String! $password: String! ) { loginUser(input: { username: $username password: $password  }) { success code token  } }")
                .variables(variables).build();
        GraphQLResponse response = graphQLWebClient.post(request).block();

        ObjectNode loginUser = response.get("loginUser", ObjectNode.class);

        if(loginUser.get("success").booleanValue()) {
            return loginUser.get("token").textValue();
        }

        throw new WrongCredentialsException();
    }

    /**
     * @param token nicht-verschlüsselter Token, der vom Backend zurückgeben wurde
     * @return verschlüsselter Token, der an Alexa gegeben werden kann
     */
    @SneakyThrows
    public String encryptToken(String token) {
        /*
        Cipher encryptCipher = Cipher.getInstance("AES");
        encryptCipher.init(Cipher.ENCRYPT_MODE, this.bringmeisterVoiceMaster.getSecretKey());
        byte[] cipherText = encryptCipher.doFinal(token.getBytes(UTF_8));

        return UriUtils.encode(Base64.getEncoder().encodeToString(cipherText), UTF_8);
         */

        return token;
    }

    /**
     * @param token verschlüsselter Token, der von Alexa kam
     * @return nicht-verschlüsselter Token, der für die Benutzung im Backend verwendet wird
     */
    @SneakyThrows
    public String decryptToken(String token) {
        /*
        byte[] bytes = Base64.getDecoder().decode(UriUtils.decode(token, UTF_8));

        Cipher decryptCitpher = Cipher.getInstance("AES");
        decryptCitpher.init(Cipher.DECRYPT_MODE, this.bringmeisterVoiceMaster.getSecretKey());

        return new String(decryptCitpher.doFinal(bytes), UTF_8);

         */

        return token;
    }

    /**
     * Diese Methode fügt im Backend ein Produkt in den Warenkorb ein.
     *
     * @param product Produkt Instanz
     * @param token nicht-verschlüsselter Token, der für die Benutzung im Backend verwendet wird
     * @throws WrongCredentialsException
     */
    public void addCartItem(Product product, String token) throws AddCartItemFailureException, WrongCredentialsException {
        HashMap<String,Object> variables = new HashMap<>();
        variables.put("sku", product.getSku());
        variables.put("unitId", product.getUnitId());
        variables.put("quantity",1);
        GraphQLRequest request = GraphQLRequest.builder()
                .query("mutation AddCartItem( $sku: String! $quantity: Float! $unitId: Int! ) { addCartItem( sku: $sku amount: { quantity: $quantity unitId: $unitId } ) { success code } }")
                .variables(variables)
                .header("authorization", "Bearer " + token)
                .operationName("AddCartItem")
                .build();

        GraphQLResponse response = graphQLWebClient.post(request).block();

        if(response.getErrors().size() != 0) {
            throw new WrongCredentialsException();
        }

        ObjectNode addCartItem = response.get("addCartItem", ObjectNode.class);
        if(!addCartItem.get("success").booleanValue()) {
            throw new AddCartItemFailureException();
        }
    }

    /**
     * Diese Methode fragt im Backend nach der PLZ des Nutzers.
     *
     * @param token nicht-verschlüsselter Token, der für die Benutzung im Backend verwendet wird
     * @return PLZ des Nutzers
     * @throws WrongCredentialsException
     */
    public String getZipCode(String token) throws WrongCredentialsException {
        GraphQLRequest request = GraphQLRequest.builder()
                .query("query GetAddressesForZipcode { user { zipcode } }")
                .header("authorization", "Bearer " + token)
                .build();

        GraphQLResponse response = graphQLWebClient.post(request).block();

        if(response.getErrors().size() != 0) {
            throw new WrongCredentialsException();
        }

        ObjectNode user = response.get("user", ObjectNode.class);
        return user.get("zipcode").textValue();
    }

    /**
     * Diese Methode fragt im Backend nach Daten des Warenkorbs (Produkte und Wert).
     * Diese werden als Objekt der Klasse Cart zurückgegeben für den weiteren Gebrauch.
     *
     * @param token nicht-verschlüsselter Token, der für die Benutzung im Backend verwendet wird
     * @return Objekt der Klasse Cart
     * @throws WrongCredentialsException
     */
    @SneakyThrows
    public Cart getCart(String token) throws WrongCredentialsException {
        GraphQLRequest request = GraphQLRequest.builder()
                .query("query GetCart { cart(keepUnavailable: true) { products { name quantity packing sku } totals { value } } }")
                .header("authorization", "Bearer " + token)
                .build();

        GraphQLResponse response = graphQLWebClient.post(request).block();

        if(response.getErrors().size() != 0) {
            throw new WrongCredentialsException();
        }

        return this.bringmeisterVoiceMaster.getObjectMapper().readerFor(Cart.class).readValue(response.get("cart", JsonNode.class));
    }

    /**
     * Diesse Methode entfernt ein Produkt vollständig aus dem Warekorb im Backend.
     *
     * @param sku ID des Produktes
     * @param token nicht-verschlüsselter Token, der für die Benutzung im Backend verwendet wird
     * @throws WrongCredentialsException
     * @throws NoProductException Heißt, dass Produkt nicht im Warenkorb ist
     */
    public void removeCartItem(String sku, String token) throws WrongCredentialsException, NoProductException {
        HashMap<String,Object> variables = new HashMap<>();
        variables.put("sku", sku);
        GraphQLRequest request = GraphQLRequest.builder()
                .query("mutation RemoveAvailabilityCartItem ( $sku: String! ) { removeCartItem(sku: $sku) { success } }")
                .variables(variables)
                .header("authorization", "Bearer " + token)
                .build();

        GraphQLResponse response = graphQLWebClient.post(request).block();

        if(response.getErrors().size() != 0) {
            throw new WrongCredentialsException();
        }

        ObjectNode removeCartItem = response.get("removeCartItem", ObjectNode.class);
        if(!removeCartItem.get("success").booleanValue()) {
            throw new NoProductException();
        }
    }

    /**
     * Diese Methode sucht einen Timeslot, der am besten zu den Anforderungen des Nutzers passt.
     *
     * @param zipCode PLZ des nutzers
     * @param day String, Tag auf Deutsch (Montag, Dienstag ...)
     * @param time Uhrzeit
     * @param token nicht-verschlüsselter Token, der für die Benutzung im Backend verwendet wird
     * @return Objekt der Klasse TimeSlot
     * @throws TimeSlotFailureException
     */
    @SneakyThrows
    public TimeSlot getTimeSlot(String zipCode, String day, String time, String token) throws TimeSlotFailureException {
        String date = TimeSlot.getDate(day);

        HashMap<String,Object> variables = new HashMap<>();
        variables.put("zipCode",zipCode);
        variables.put("startDate", date);
        variables.put("first", 1);

        GraphQLRequest request = GraphQLRequest.builder()
                .query("query GetCapacities( $zipCode: String $startDate: Date $first: Int! ) { capacities( zipCode: $zipCode startDate: $startDate, first: $first ) { edges { node { date slots { slotId price startDateTime endDateTime } } } } }")
                .variables(variables)
                .header("authorization", "Bearer " + token)
                .build();

        GraphQLResponse response = BringmeisterVoiceMaster.getInstance().getUserService().getGraphQLWebClient().post(request).block();

        if(response.getErrors().size() != 0) {
            throw new TimeSlotFailureException();
        }

        Iterator<JsonNode> slots = response.get("capacities", ObjectNode.class).get("edges").get(0).get("node").get("slots").iterator();
        TimeSlot timeSlot = new TimeSlot();

        while (slots.hasNext() && timeSlot.getId() == null) {
            ObjectNode slot = (ObjectNode) slots.next();
            if(slot.get("price").intValue() < 999) {
                Date startDate = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")).parse(slot.get("startDateTime").textValue());
                Date endDate = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")).parse(slot.get("endDateTime").textValue());
                String startTime = new SimpleDateFormat("HH:mm").format(startDate);
                String endTime = new SimpleDateFormat("HH:mm").format(endDate);
                if(startTime.compareTo(time) >= 0 || endTime.compareTo(time) >= 0) {
                    timeSlot.setId(slot.get("slotId").textValue());
                    timeSlot.setTimeStart(startTime);
                    timeSlot.setTimeEnd(endTime);
                    timeSlot.setPrice(Math.round(slot.get("price").doubleValue()*100.0)/100.0);
                    timeSlot.setDay(day);
                }
            }
        }

        if(timeSlot.getId() == null) {
            throw new TimeSlotFailureException();
        }

        return timeSlot;
    }

    /**
     * Diese Methode änder die Lieferschiene im Backend.
     *
     * @param timeSlotId ID der Zeitschiene, die ausgewählt werden soll
     * @param token nicht-verschlüsselter Token, der für die Benutzung im Backend verwendet wird
     * @throws TimeSlotFailureException
     */
    public void setTimeSlot(String timeSlotId, String token) throws TimeSlotFailureException {
        HashMap<String,Object> variables = new HashMap<>();
        variables.put("oldSlotId", timeSlotId);

        GraphQLRequest request = GraphQLRequest.builder()
                .query("mutation SetReservedSlot( $oldSlotId: String!) { setReservedSlot(oldSlotId: $oldSlotId) { success } }")
                .header("authorization", "Bearer " + token)
                .variables(variables)
                .build();

        GraphQLResponse response = BringmeisterVoiceMaster.getInstance().getUserService().getGraphQLWebClient().post(request).block();

        ObjectNode setReservedSlot = response.get("setReservedSlot", ObjectNode.class);

        if(!setReservedSlot.get("success").booleanValue()) {
            throw new TimeSlotFailureException();
        }
    }

}
