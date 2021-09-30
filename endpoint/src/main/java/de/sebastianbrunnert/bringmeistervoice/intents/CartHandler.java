package de.sebastianbrunnert.bringmeistervoice.intents;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.RequestHelper;
import de.sebastianbrunnert.bringmeistervoice.BringmeisterVoiceMaster;
import de.sebastianbrunnert.bringmeistervoice.bean.Cart;
import de.sebastianbrunnert.bringmeistervoice.exceptions.WrongCredentialsException;

import java.util.List;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

/**
 * RequestHandler gemäß dem Aufbau des Alexa SDK.
 * Diese Anfrage wird gestellt, wenn der Nutzer Informationen über seinen Warenkorb erhalten möchte.
 *
 * @author Sebastian Brunnert
 */
public class CartHandler implements IntentRequestHandler {
    @Override
    public boolean canHandle(HandlerInput handlerInput, IntentRequest intentRequest) {
        return handlerInput.matches(intentName("CartIntent"));
    }

    /**
     * Die Anfrage kann verarbeitet werden. Mithilfe des UserService werden Eckdaten über den Warenkorb erfragt und
     * ausgegeben. Der Nutzer kann jetzt Ja oder Nein antwortet, ob er eine Auflistung haben möchte.
     *
     * @param handlerInput
     * @param intentRequest
     * @return
     */
    @Override
    public Optional<Response> handle(HandlerInput handlerInput, IntentRequest intentRequest) {
        RequestHelper requestHelper = RequestHelper.forHandlerInput(handlerInput);
        try {
            Cart cart = BringmeisterVoiceMaster.getInstance().getUserService().getCart(requestHelper.getAccountLinkingAccessToken());
            String speech;

            if(cart.getProducts().size() == 0) {
                speech = "Du hast keine Produkte in deinem Warenkorb.";
            } else if(cart.getProducts().size() == 1) {
                speech = "Du hast ein Produkt in deinem Warenkorb: " + cart.getProducts().get(0).getQuantity() + " mal " + cart.getProducts().get(0).getName() + " " + cart.getProducts().get(0).getPacking() + " im Wert von " + cart.getTotals().get(0).getValue() + "€";
            } else {
                handlerInput.getAttributesManager().getSessionAttributes().put("QUESTION","CartIntent");
                handlerInput.getAttributesManager().getSessionAttributes().put("PRODUCTS", cart.getProducts());
                speech = "Du hast " + cart.getProducts().size() + " Produkte im Wert von " + cart.getTotals().get(0).getValue() + "€ in deinem Warenkorb. Soll Ich sie auflisten?";
            }

            return handlerInput.getResponseBuilder()
                    .withShouldEndSession(false)
                    .withSpeech(speech)
                    .withSimpleCard("Bringmeister", cart.getTotals().get(0).getValue() + "€")
                    .build();
        } catch (WrongCredentialsException e) {
            return handlerInput.getResponseBuilder()
                    .withShouldEndSession(true)
                    .withSpeech("Entschuldigung. Du bist nicht mit Bringmeister verbunden. Bitte erledige das in der Alexa App.")
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
                    && handlerInput.getAttributesManager().getSessionAttributes().get("QUESTION").equals("CartIntent");
        }

        /**
         * Die Anfrage kann verarbeitet werden. Die Produkte werden nun aufgelistet.
         *
         * @param handlerInput
         * @param intentRequest
         * @return
         */
        @Override
        public Optional<Response> handle(HandlerInput handlerInput, IntentRequest intentRequest) {
            RequestHelper requestHelper = RequestHelper.forHandlerInput(handlerInput);
            List<Object> cartProducts = BringmeisterVoiceMaster.getInstance().getObjectMapper().convertValue(handlerInput.getAttributesManager().getSessionAttributes().get("PRODUCTS"), List.class);

            handlerInput.getAttributesManager().getSessionAttributes().remove("QUESTION");
            handlerInput.getAttributesManager().getSessionAttributes().remove("PRODUCTS");

            String speech = "";

            for(Object obj : cartProducts) {
                Cart.CartProduct cartProduct = BringmeisterVoiceMaster.getInstance().getObjectMapper().convertValue(obj, Cart.CartProduct.class);
                speech += cartProduct.getQuantity().replace("1","Ein") + " mal " + cartProduct.getName() + " " + cartProduct.getPacking() + ". ";
            }

            return handlerInput.getResponseBuilder()
                    .withSpeech(speech)
                    .withShouldEndSession(false)
                    .build();
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
                    && handlerInput.getAttributesManager().getSessionAttributes().get("QUESTION").equals("CartIntent");
        }

        /**
         * Die Produkte werden nicht aufgelistet.
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
                    .withSpeech("Alles klar.")
                    .build();
        }
    }
}
