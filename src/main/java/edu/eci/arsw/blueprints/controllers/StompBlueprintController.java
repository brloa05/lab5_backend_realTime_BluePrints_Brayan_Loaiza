package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.model.BlueprintUpdate;
import edu.eci.arsw.blueprints.model.DrawEvent;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class StompBlueprintController {

    private final SimpMessagingTemplate template;
    private final BlueprintsServices services;

    public StompBlueprintController(SimpMessagingTemplate template, BlueprintsServices services) {
        this.template = template;
        this.services = services;
    }

    @MessageMapping("/draw")
    public void onDraw(DrawEvent evt) {
        List<Point> points;
        try {
            services.addPoint(evt.author(), evt.name(), evt.point().x(), evt.point().y());
            points = services.getBlueprint(evt.author(), evt.name()).getPoints();
        } catch (Exception ignored) {
            points = List.of(evt.point());
        }
        var upd = new BlueprintUpdate(evt.author(), evt.name(), points);
        template.convertAndSend("/topic/blueprints." + evt.author() + "." + evt.name(), upd);
    }
}
