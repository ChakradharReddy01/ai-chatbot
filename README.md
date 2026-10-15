# 🤖 AI Chatbot with Spring AI & Groq

A lightweight, high-performance RESTful AI Chatbot backend built using **Spring Boot** and **Spring AI**, powered by ultra-fast LLM inference via the **Groq API** (OpenAI-compatible endpoint).

---

## 🚀 Features

- **Spring AI Integration**: Uses Spring AI's modern `ChatClient` fluent API for seamless LLM prompt orchestration.
- **Groq Cloud Inference**: Powered by Groq's high-speed LPU engine via OpenAI-compatible endpoints.
- **Clean REST Architecture**: Simple and extendable RESTful endpoint (`/api/chat`) accepting JSON payloads and returning structured answers.
- **Secure Configuration**: Uses environment variables (`GROQ_API_KEY`) to prevent leaking secrets in source code.
- **UTF-8 Enforced**: Configured servlet response encoding to preserve special characters, emojis, and multilingual text.

---

## 🛠️ Tech Stack

| Technology | Version / Description |
|---|---|
| **Java** | 24 |
| **Spring Boot** | 4.1.x |
| **Spring AI** | 2.0.1 (`spring-ai-starter-model-openai`) |
| **AI Provider** | Groq Cloud (`openai/gpt-oss-120b` or any Groq model) |
| **Build Tool** | Maven (with wrapper `mvnw`) |

---

## 📁 Project Structure

```text
ChatBot/
├── .gitignore
├── README.md
└── ai-chatbot/
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    └── src/
        ├── main/
        │   ├── java/com/spring/ai_chatbot/
        │   │   ├── AiChatbotApplication.java    # Application entry point
        │   │   ├── controller/
        │   │   │   └── ChatController.java       # POST /api/chat endpoint
        │   │   └── service/
        │   │       └── ChatService.java          # Spring AI ChatClient service
        │   └── resources/
        │       └── application.properties       # Groq & server configurations
        └── test/
            └── java/com/spring/ai_chatbot/
                └── AiChatbotApplicationTests.java
```

---

## ⚙️ Prerequisites

1. **Java Development Kit (JDK) 24** or later installed.
2. **Groq API Key**:
   - Sign up for a free key at [Groq Console](https://console.groq.com/keys).

---

## 🔑 Setup & Installation

### 1. Clone the Repository
```bash
git clone https://github.com/ChakradharReddy01/ai-chatbot.git
cd ai-chatbot
```

### 2. Set the Environment Variable
Set your Groq API key in your system or terminal session:

- **Windows (PowerShell):**
  ```powershell
  $env:GROQ_API_KEY="your_actual_groq_api_key_here"
  ```

- **Windows (Command Prompt):**
  ```cmd
  set GROQ_API_KEY=your_actual_groq_api_key_here
  ```

- **Linux / macOS (Bash / Zsh):**
  ```bash
  export GROQ_API_KEY="your_actual_groq_api_key_here"
  ```

---

## 🏃 Running the Application

Navigate into the `ai-chatbot` project directory and start the Spring Boot application using Maven:

- **Windows (PowerShell / CMD):**
  ```powershell
  cd ai-chatbot
  .\mvnw.cmd spring-boot:run
  ```

- **Linux / macOS:**
  ```bash
  cd ai-chatbot
  ./mvnw spring-boot:run
  ```

The server will start on port `8080` (e.g., `http://localhost:8080`).

---

## 📡 API Reference

### Send a Chat Message

- **URL:** `POST /api/chat`
- **Content-Type:** `application/json`

#### Request Body
```json
{
  "message": "Explain quantum computing in simple terms."
}
```

#### Response Body
```json
{
  "response": "Quantum computing is a type of computing that uses quantum bits (qubits) instead of classical bits (0s and 1s)..."
}
```

---

## 🧪 Testing the API

### Option A: Using cURL
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"message\": \"Hello! What can you help me with?\"}"
```

### Option B: Using PowerShell
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/chat" `
  -Method Post `
  -Headers @{ "Content-Type" = "application/json" } `
  -Body '{"message": "Hello! What can you help me with?"}'
```

### Option C: Using Postman or Insomnia
- **Method:** `POST`
- **URL:** `http://localhost:8080/api/chat`
- **Headers:** `Content-Type: application/json`
- **Body (raw JSON):**
  ```json
  {
    "message": "Tell me a fun programming joke!"
  }
  ```

---

## 🔧 Configuration Details

Configuration properties are located in [application.properties](file:///d:/ChatBot/ai-chatbot/src/main/resources/application.properties):

| Property | Default Value | Description |
|---|---|---|
| `server.port` | `8080` | Port on which the Spring Boot application runs |
| `spring.ai.openai.base-url` | `https://api.groq.com/openai/v1` | Groq's OpenAI-compatible base URL |
| `spring.ai.openai.api-key` | `${GROQ_API_KEY}` | Injected via environment variable |
| `spring.ai.openai.chat.options.model` | `openai/gpt-oss-120b` | Model identifier used for chat completions |

---

## 👤 Author

- **Chakradhar Reddy** - [GitHub Profile](https://github.com/ChakradharReddy01)