package de.sebastianbrunnert.bringmeistervoice.intents;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.request.RequestHelper;
import de.sebastianbrunnert.bringmeistervoice.BringmeisterVoiceMaster;
import de.sebastianbrunnert.bringmeistervoice.bean.Product;
import de.sebastianbrunnert.bringmeistervoice.bean.TimeSlot;
import de.sebastianbrunnert.bringmeistervoice.exceptions.AddCartItemFailureException;
import de.sebastianbrunnert.bringmeistervoice.exceptions.TimeSlotFailureException;
import de.sebastianbrunnert.bringmeistervoice.exceptions.WrongCredentialsException;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

/**
 * RequestHandler gemäß dem Aufbau des Alexa SDK.
 * Diese Anfrage wird gestellt, wenn der Nutzer die Lieferzeiten einstellen möchte
 *
 * @author Sebastian Brunnert
 */
public class TimeSlotHandler implements RequestHandler {
    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(intentName("TimeSlotIntent"));
    }

    /**
     * Die Anfrage kann verarbeitet werden. Es wird über den DataService nach möglichen Slots gesucht.
     * Der passenste wird ausgegeben, um nach Verifikation durch den Nutzer zu fragen.
     *
     * @param handlerInput
     * @return
     */
    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        RequestHelper requestHelper = RequestHelper.forHandlerInput(handlerInput);
        String day = "";
        String time = "";

        try {
            day = requestHelper.getSlot("day").get().getResolutions().getResolutionsPerAuthority().get(0).getValues().get(0).getValue().getName();
        } catch (Exception e) {
            return handlerInput.getResponseBuilder()
                    .withSpeech("Entschuldigung. Diesen Tag kenne Ich nicht.")
                    .withShouldEndSession(false)
                    .build();
        }

        try {
            time = requestHelper.getSlot("time").get().getValue();
        } catch (Exception e) {
            return handlerInput.getResponseBuilder()
                    .withSpeech("Entschuldigung. Diese Uhrzeit kenne Ich nicht.")
                    .withShouldEndSession(false)
                    .build();
        }


        try {
            TimeSlot timeSlot = BringmeisterVoiceMaster.getInstance().getUserService().getTimeSlot(
                    BringmeisterVoiceMaster.getInstance().getUserService().getZipCode(requestHelper.getAccountLinkingAccessToken()),
                    day,time,requestHelper.getAccountLinkingAccessToken());

            handlerInput.getAttributesManager().getSessionAttributes().put("QUESTION","TimeSlotIntent");
            handlerInput.getAttributesManager().getSessionAttributes().put("TIMESLOT", timeSlot.getId());

            return handlerInput.getResponseBuilder()
                    .withSpeech("Soll Bringmeister " + timeSlot.getDay() + " zwischen " + timeSlot.getTimeStart() + " und " + timeSlot.getTimeEnd() + " bei dir liefern? Das kostet " + timeSlot.getPrice() + "€.")
                    .withShouldEndSession(false)
                    .build();
        } catch (TimeSlotFailureException | WrongCredentialsException e) {
            return handlerInput.getResponseBuilder()
                    .withSpeech("Entschuldigung. Ich habe keine passende Lieferzeit gefunden.")
                    .withShouldEndSession(false)
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
                    && handlerInput.getAttributesManager().getSessionAttributes().get("QUESTION").equals("TimeSlotIntent");
        }

        /**
         * Die Anfrage kann verarbeitet werden. Die Zeitschiene wird ausewählt.
         *
         * @param handlerInput
         * @param intentRequest
         * @return
         */
        @Override
        public Optional<Response> handle(HandlerInput handlerInput, IntentRequest intentRequest) {
            RequestHelper requestHelper = RequestHelper.forHandlerInput(handlerInput);
            String timeSlotId = (String) handlerInput.getAttributesManager().getSessionAttributes().get("TIMESLOT");

            handlerInput.getAttributesManager().getSessionAttributes().remove("QUESTION");
            handlerInput.getAttributesManager().getSessionAttributes().remove("TIMESLOT");


            try {
                BringmeisterVoiceMaster.getInstance().getUserService().setTimeSlot(timeSlotId,requestHelper.getAccountLinkingAccessToken());
                return handlerInput.getResponseBuilder()
                        .withShouldEndSession(false)
                        .withSimpleCard("Bringmeister", "Lieferzeit geändert")
                        .withSpeech("Ist erledigt!")
                        .build();
            } catch (TimeSlotFailureException e) {
                return handlerInput.getResponseBuilder()
                        .withSpeech("Entschuldigung. Ich habe keine passende Lieferzeit gefunden.")
                        .withShouldEndSession(false)
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
                    && handlerInput.getAttributesManager().getSessionAttributes().get("QUESTION").equals("TimeSlotIntent");
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
            handlerInput.getAttributesManager().getSessionAttributes().remove("TIMESLOT");
            return handlerInput.getResponseBuilder()
                    .withShouldEndSession(false)
                    .withSpeech("Alles klar..")
                    .build();
        }
    }
}
