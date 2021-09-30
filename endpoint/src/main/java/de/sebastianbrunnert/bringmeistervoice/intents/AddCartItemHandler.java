package de.sebastianbrunnert.bringmeistervoice.intents;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.request.RequestHelper;
import de.sebastianbrunnert.bringmeistervoice.BringmeisterVoiceMaster;
import de.sebastianbrunnert.bringmeistervoice.bean.Product;
import de.sebastianbrunnert.bringmeistervoice.exceptions.AddCartItemFailureException;
import de.sebastianbrunnert.bringmeistervoice.exceptions.NoProductException;
import de.sebastianbrunnert.bringmeistervoice.exceptions.WrongCredentialsException;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

/**
 * RequestHandler gemäß dem Aufbau des Alexa SDK.
 * Diese Anfrage wird gestellt, wenn der Nutzer eine Suchanfrage zum Hinzufügen eines Produktes in den Warenkorb stellt.
 *
 * @author Sebastian Brunnert
 */
public class AddCartItemHandler implements IntentRequestHandler {
    @Override
    public boolean canHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return handlerInput.matches(intentName("AddCartItemIntent"));
    }

    /**
     * Die Anfrage kann verarbeitet werden. Es wird das Produkt mithilfe des DataService gesucht und ausgegeben. Der Nutzer
     * kann jetzt Ja oder Nein antworten.
     *
     * @param handlerInput
     * @param intentRequest
     * @return
     */
    @Override
    public Optional<Response> handle(HandlerInput handlerInput, IntentRequest intentRequest) {
        RequestHelper requestHelper = RequestHelper.forHandlerInput(handlerInput);
        Optional<Slot> slot = requestHelper.getSlot("query");

        // Dürfte eigentlich nicht passieren
        if(!slot.isPresent()) {
            return handlerInput.getResponseBuilder()
                    .withShouldEndSession(false)
                    .withSpeech("Entschuldigung. Das habe Ich nicht verstanden.")
                    .build();
        }

        try {
            try {
                String zipCode = BringmeisterVoiceMaster.getInstance().getUserService().getZipCode(requestHelper.getAccountLinkingAccessToken());

                Product product = BringmeisterVoiceMaster.getInstance().getDataService().getFirstProduct(
                        slot.get().getValue(), zipCode
                );

                String speech = BringmeisterVoiceMaster.getInstance().getDataService().isGoodWeather(zipCode) ?
                        "Es ist gerade gutes Wetter, du kannst auch selber einkaufen gehen. Soll Ich trotzdem " + product.getName() + " " + product.getPacking() + " in deinen Warenkorb legen?"
                        : "Soll Ich " + product.getName() + " " + product.getPacking() + " in deinen Warenkorb legen?";

                handlerInput.getAttributesManager().getSessionAttributes().put("QUESTION","AddCartItemIntent");
                handlerInput.getAttributesManager().getSessionAttributes().put("PRODUCT", product);
                return handlerInput.getResponseBuilder()
                        .withShouldEndSession(false)
                        .withSpeech(speech)
                        .withReprompt("Soll Ich " + product.getName() + " " + product.getPacking() + " in deinen Warenkorb legen?")
                        .withSimpleCard("Bringmeister",product.getName() + "?")
                        .build();
            } catch (WrongCredentialsException e) {
                return handlerInput.getResponseBuilder()
                        .withShouldEndSession(true)
                        .withSpeech("Entschuldigung. Du bist nicht mit Bringmeister verbunden. Bitte erledige das in der Alexa App.")
                        .build();
            }
        } catch (NoProductException e) {
            return handlerInput.getResponseBuilder()
                    .withShouldEndSession(false)
                    .withSpeech("Entschuldigung. Das Produkt habe Ich nicht gefunden. Versuche es noch einmal.")
                    .build();
        }
    }

    /**
     * RequestHandler gemäß dem Aufbau des Alexa SDK.
     * Diese Anfrage wird gestellt, wenn der Nutzer "Ja" antwortet.
     *
     * @author Sebastian Brunnert
     */
    public static class YesHandler implements IntentRequestHandler {
        @Override
        public boolean canHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
            return handlerInput.matches(intentName("AMAZON.YesIntent"))
                    && handlerInput.getAttributesManager().getSessionAttributes().containsKey("QUESTION")
                    && handlerInput.getAttributesManager().getSessionAttributes().get("QUESTION").equals("AddCartItemIntent");
        }

        /**
         * Die Anfrage kann verarbeitet werden. Das Produkt wird nun in den Warenkorb hinzugefügt.
         *
         * @param handlerInput
         * @param intentRequest
         * @return
         */
        @Override
        public Optional<Response> handle(HandlerInput handlerInput, IntentRequest intentRequest) {
            RequestHelper requestHelper = RequestHelper.forHandlerInput(handlerInput);
            Product product = BringmeisterVoiceMaster.getInstance().getObjectMapper().convertValue(handlerInput.getAttributesManager().getSessionAttributes().get("PRODUCT"), Product.class);

            handlerInput.getAttributesManager().getSessionAttributes().remove("QUESTION");
            handlerInput.getAttributesManager().getSessionAttributes().remove("PRODUCT");

            try {
                try {
                    BringmeisterVoiceMaster.getInstance().getUserService().addCartItem(product, requestHelper.getAccountLinkingAccessToken());
                    return handlerInput.getResponseBuilder()
                            .withShouldEndSession(false)
                            .withSimpleCard("Bringmeister", product.getName() + " im Warenkorb!")
                            .withSpeech("Ist hinzugefügt!")
                            .build();
                } catch (WrongCredentialsException e) {
                    return handlerInput.getResponseBuilder()
                            .withShouldEndSession(true)
                            .withSpeech("Entschuldigung. Du bist nicht mit Bringmeister verbunden. Bitte erledige das in der Alexa App.")
                            .build();
                }
            } catch (AddCartItemFailureException e) {
                return handlerInput.getResponseBuilder()
                        .withShouldEndSession(false)
                        .withSpeech("Entschuldigung. Ein Fehler ist aufgetreten. Versuche es noch einmal.")
                        .build();
            }
        }
    }

    /**
     * RequestHandler gemäß dem Aufbau des Alexa SDK.
     * Diese Anfrage wird gestellt, wenn der Nutzer "Nein" antwortet.
     *
     * @author Sebastian Brunnert
     */
    public static class NoHandler implements IntentRequestHandler {
        @Override
        public boolean canHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
            return handlerInput.matches(intentName("AMAZON.NoIntent"))
                    && handlerInput.getAttributesManager().getSessionAttributes().containsKey("QUESTION")
                    && handlerInput.getAttributesManager().getSessionAttributes().get("QUESTION").equals("AddCartItemIntent");
        }

        /**
         * Die Anfrage kann verarbeitet werden.
         *
         * @param handlerInput
         * @param intentRequest
         * @return
         */
        @Override
        public Optional<Response> handle(HandlerInput handlerInput, IntentRequest intentRequest) {
            handlerInput.getAttributesManager().getSessionAttributes().remove("QUESTION");
            handlerInput.getAttributesManager().getSessionAttributes().remove("PRODUCT");
            return handlerInput.getResponseBuilder()
                    .withShouldEndSession(false)
                    .withSpeech("Alles klar. Versuche ein anderes Produkt in deinen Warenkorb zu legen.")
                    .build();
        }
    }
}
