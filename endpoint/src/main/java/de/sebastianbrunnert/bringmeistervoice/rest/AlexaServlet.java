package de.sebastianbrunnert.bringmeistervoice.rest;

import com.amazon.ask.Skill;
import com.amazon.ask.Skills;
import com.amazon.ask.servlet.SkillServlet;
import de.sebastianbrunnert.bringmeistervoice.intents.*;

public class AlexaServlet extends SkillServlet {
    public AlexaServlet() {
        super(getSkill());
    }

    public static Skill getSkill() {
        return Skills.standard()
                .addRequestHandlers(
                        new AddCartItemHandler(),
                        new AddCartItemHandler.YesHandler(),
                        new AddCartItemHandler.NoHandler(),

                        new CartHandler(),
                        new CartHandler.YesHandler(),
                        new CartHandler.NoHandler(),

                        new RemoveCartItemHandler(),
                        new RemoveCartItemHandler.YesHandler(),
                        new RemoveCartItemHandler.NoHandler(),

                        new TimeSlotHandler(),
                        new TimeSlotHandler.YesHandler(),
                        new TimeSlotHandler.NoHandler(),

                        new LaunchRequestHandler(),
                        new HelpIntentHandler(),
                        new CancelAndStopIntentHandler(),
                        new UnhandledHandler())
                .build();
    }
}
