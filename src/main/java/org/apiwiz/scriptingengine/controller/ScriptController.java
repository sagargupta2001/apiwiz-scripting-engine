package org.apiwiz.scriptingengine.controller;

import org.apiwiz.scriptingengine.service.ScriptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.apiwiz.scriptingengine.dto.ScriptRequest;
import org.apiwiz.scriptingengine.dto.ScriptResponse;

@RestController
@RequestMapping("/api/scripts")
public class ScriptController {
    private final ScriptService scriptService;

    public ScriptController(ScriptService scriptService) {
        this.scriptService = scriptService;
    }

    @PostMapping("/execute")
    public ResponseEntity<ScriptResponse> execute(@RequestBody ScriptRequest request) {
        return ResponseEntity.ok(scriptService.executeScript(request.language(), request.script()));
    }
}
