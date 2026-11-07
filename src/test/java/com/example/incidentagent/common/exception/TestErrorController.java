package com.example.incidentagent.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoints that raise each kind of failure so the error envelope can be verified. */
@RestController
@RequestMapping("/api/test-errors")
public class TestErrorController {

    public record Payload(@NotBlank String name, @Min(1) int count) {
    }

    @PostMapping("/validate")
    public String validate(@Valid @RequestBody Payload payload) {
        return "ok";
    }

    @GetMapping("/number/{n}")
    public String number(@PathVariable long n) {
        return "ok";
    }

    @GetMapping("/state")
    public String state() {
        throw new InvalidStateException("Incident has actions with no recorded outcome");
    }

    @GetMapping("/memory")
    public String memory() {
        throw new MemoryUnavailableException("Hindsight is unreachable");
    }

    @GetMapping("/llm")
    public String llm() {
        throw new LlmUnavailableException("Groq failed");
    }

    @GetMapping("/boom")
    public String boom() {
        throw new IllegalStateException("secret internal detail");
    }
}
