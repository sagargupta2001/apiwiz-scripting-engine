package org.apiwiz.scriptingengine.controller;

import org.apiwiz.scriptingengine.models.ScriptLanguage;
import org.apiwiz.scriptingengine.service.ScriptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apiwiz.scriptingengine.dto.ScriptRequest;
import org.apiwiz.scriptingengine.dto.ScriptResponse;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/execute-file")
    public ResponseEntity<ScriptResponse> executeFromFile(
            @RequestParam("language") String language,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(scriptService.executeUploadedScriptFile(language, file));
    }
}
