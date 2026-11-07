# IncidentIQ – AI-Powered Incident Response Agent

IncidentIQ is an AI-powered incident response system designed to help engineers investigate, analyze, and resolve production incidents more effectively. It combines Large Language Model (LLM) reasoning with persistent historical memory to provide context-aware recommendations based on previous incident experiences.

Unlike conventional monitoring systems that simply report failures, IncidentIQ learns from past incident investigations, remembers successful and unsuccessful remediation attempts, and retrieves relevant knowledge when similar incidents occur again.

## Key Features

- **Incident Detection & Analysis:** Analyze incoming alerts, logs, affected services, and severity levels.
- **AI-Powered Reasoning:** Uses Groq LLM to investigate incidents and generate structured response recommendations.
- **Historical Memory:** Integrates Hindsight Memory to store and retrieve previous incident experiences.
- **Context-Aware Recommendations:** Identifies similar historical incidents and suggests evidence-backed remediation steps.
- **Learning from Outcomes:** Records successful and unsuccessful troubleshooting attempts to improve future recommendations.
- **Incident Management:** Track incidents, investigation history, and remediation outcomes.
- **Feedback System:** Collect user feedback to support continuous platform improvement.

## Technology Stack

- **Backend:** Java, Spring Boot
- **Frontend:** React
- **Database:** MySQL
- **AI/LLM:** Groq API (GPT-OSS 20B)
- **Memory:** Hindsight Cloud API
- **Architecture:** REST API-based client-server architecture

## How It Works

1. **Observe:** Receive incident alerts and analyze system logs.
2. **Recall:** Retrieve similar historical incidents and previously recorded solutions.
3. **Reason:** Use the Groq LLM to analyze the available evidence and generate recommendations.
4. **Guide:** Present recommended next steps for engineers to review and choose.

**IncidentIQ keeps engineers in control:** it provides intelligent recommendations rather than blindly executing potentially dangerous production fixes.

## Project Vision

To build an intelligent incident response assistant that captures operational knowledge, reduces repetitive troubleshooting, and helps engineering teams resolve recurring production issues faster.
