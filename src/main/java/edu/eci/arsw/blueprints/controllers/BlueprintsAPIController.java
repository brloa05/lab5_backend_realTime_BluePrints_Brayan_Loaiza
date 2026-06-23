package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/blueprints")
@CrossOrigin(origins = "*")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) { this.services = services; }

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(required = false) String author) {
        try {
            if (author != null) return ResponseEntity.ok(services.getBlueprintsByAuthor(author));
            return ResponseEntity.ok(services.getAllBlueprints());
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{author}/{name}")
    public ResponseEntity<?> getByAuthorAndName(@PathVariable String author, @PathVariable String name) {
        try {
            return ResponseEntity.ok(services.getBlueprint(author, name));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody NewBlueprintRequest req) {
        try {
            services.addNewBlueprint(new Blueprint(req.author(), req.name(), req.points()));
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (BlueprintPersistenceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/{author}/{name}")
    public ResponseEntity<?> update(@PathVariable String author, @PathVariable String name,
                                    @RequestBody UpdateBlueprintRequest req) {
        try {
            services.updateBlueprint(author, name, req.points());
            return ResponseEntity.ok().build();
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{author}/{name}")
    public ResponseEntity<?> delete(@PathVariable String author, @PathVariable String name) {
        try {
            services.deleteBlueprint(author, name);
            return ResponseEntity.noContent().build();
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    public record NewBlueprintRequest(String author, String name, List<Point> points) {}
    public record UpdateBlueprintRequest(List<Point> points) {}
}
