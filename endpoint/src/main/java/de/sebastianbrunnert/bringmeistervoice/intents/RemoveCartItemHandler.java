package de.sebastianbrunnert.bringmeistervoice.intents;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.request.RequestHelper;
import de.sebastianbrunnert.bringmeistervoice.BringmeisterVoiceMaster;
import de.sebastianbrunnert.bringmeistervoice.bean.Cart;
import de.sebastianbrunnert.bringmeistervoice.bean.Product;
import de.sebastianbrunnert.bringmeistervoice.exceptions.AddCartItemFailureException;
import de.sebastianbrunnert.bringmeistervoice.exceptions.NoProductException;
import de.sebastianbrunnert.bringmeistervoice.exceptions.WrongCredentialsException;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

/**
 * RequestHandler gemäß dem Aufbau des Alexa SDK.
 * Diese Anfrage wird gestellt, wenn der Nutzer eine Suchanfrage zum Entfernen eines Produktes aus dem Warenkorb stellt.
 *
 * @author Sebastian Brunnert
 */
public class RemoveCartItemHandler implements IntentRequestHandler {
    @Override
    public boolean canHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return handlerInput.matches(intentName("RemoveCartItemIntent"));
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
                Product product = BringmeisterVoiceMaster.getInstance().getDataService().getFirstProduct(
                        slot.get().getValue(),
                        BringmeisterVoiceMaster.getInstance().getUserService().getZipCode(requestHelper.getAccountLinkingAccessToken())
                );

                Cart cart = BringmeisterVoiceMaster.getInstance().getUserService().getCart(requestHelper.getAccountLinkingAccessToken());

                Optional<Cart.CartProduct> match = cart.getProducts().stream().filter(cartProduct -> cartProduct.getSku().equals(product.getSku())).findFirst();

                if(match.isPresent()) {
                    handlerInput.getAttributesManager().getSessionAttributes().put("QUESTION","RemoveCartItemIntent");
                    handlerInput.getAttributesManager().getSessionAttributes().put("PRODUCT_SKU", product.getSku());
                    return handlerInput.getResponseBuilder()
                            .withShouldEndSession(false)
                            .withSpeech("Soll Ich " + match.get().getQuantity() + " mal " + product.getName() + " " + product.getPacking() + " aus deinem Warenkorb entfernen?")
                            .withReprompt("Soll Ich " + product.getName() + " " + product.getPacking() + " aus deinem Warenkorb entfernen?")
                            .withSimpleCard("Bringmeister",product.getName() + " entfernen?")
                            .build();
                }
            } catch (WrongCredentialsException e) {
                return handlerInput.getResponseBuilder()
                        .withShouldEndSession(true)
                        .withSpeech("Entschuldigung. Du bist nicht mit Bringmeister verbunden. Bitte erledige das in der Alexa App.")
                        .build();
            }
        } catch (NoProductException e) { }

        return handlerInput.getResponseBuilder()
                .withShouldEndSession(false)
                .withSpeech("Entschuldigung. Das Produkt habe Ich nicht gefunden. Versuche es noch einmal.")
                .build();
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
                    && handlerInput.getAttributesManager().getSessionAttributes().get("QUESTION").equals("RemoveCartItemIntent");
        }

        /**
         * Die Anfrage kann verarbeitet werden. Das Produkt wird nun aus dem Warenkorb entfernt.
         *
         * @param handlerInput
         * @param intentRequest
         * @return
         */
        @Override
        public Optional<Response> handle(HandlerInput handlerInput, IntentRequest intentRequest) {
            RequestHelper requestHelper = RequestHelper.forHandlerInput(handlerInput);
            String sku = (String) handlerInput.getAttributesManager().getSessionAttributes().get("PRODUCT_SKU");

            handlerInput.getAttributesManager().getSessionAttributes().remove("QUESTION");
            handlerInput.getAttributesManager().getSessionAttributes().remove("PRODUCT_SKU");

            try {
                try {
                    BringmeisterVoiceMaster.getInstance().getUserService().removeCartItem(sku, requestHelper.getAccountLinkingAccessToken());
                    return handlerInput.getResponseBuilder()
                            .withShouldEndSession(false)
                            .withSimpleCard("Bringmeister", "Produkt entfernt")
                            .withSpeech("Ist entfernt!")
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
                    && handlerInput.getAttributesManager().getSessionAttributes().get("QUESTION").equals("RemoveCartItemIntent");
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
            handlerInput.getAttributesManager().getSessionAttributes().remove("PRODUCT_SKU");
            return handlerInput.getResponseBuilder()
                    .withShouldEndSession(false)
                    .withSpeech("Alles klar. Versuche ein anderes Produkt aus deinem Warenkorb zu entfernen.")
                    .build();
        }
    }
}
