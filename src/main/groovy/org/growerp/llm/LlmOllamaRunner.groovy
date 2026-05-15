import org.moqui.util.RestClient
import org.moqui.context.ExecutionContext

ExecutionContext ec = context.ec

// Default to gemma4 if not explicitly provided
String model = context.modelName ?: "gemma4" 
// Fetch API URL from system property or default to local Ollama
String ollamaEndpoint = ec.user.getPreference("OLLAMA_API_URL") ?: "http://localhost:11434/api/chat"

// Prepare the payload for Ollama
def payload = [
    model: model,
    messages: context.messages,
    stream: false,
    options: [
        temperature: context.temperature ?: 0.7
    ]
]

// Make the HTTP POST request to the local server
RestClient restClient = ec.resource.restClient()
    .uri(ollamaEndpoint)
    .method("POST")
    .addHeader("Content-Type", "application/json")
    .jsonObject(payload)

RestClient.RestResponse response = restClient.call()

if (response.statusCode < 200 || response.statusCode >= 300) {
    ec.message.addError("Error calling Local LLM: ${response.text()}")
    return
}

def responseMap = response.jsonObject()

// Standardize the output to match the agnostic interface
result.reply = responseMap.message?.content
result.usage = [
    promptTokens: responseMap.prompt_eval_count,
    completionTokens: responseMap.eval_count,
    totalTokens: (responseMap.prompt_eval_count ?: 0) + (responseMap.eval_count ?: 0)
]
