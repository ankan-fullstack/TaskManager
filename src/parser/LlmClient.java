package parser;

/**
 * Interface for LLM communication abstraction.
 * Instructor scaffold representing the network client for calling AI models.
 */
public interface LlmClient {

    /**
     * Sends a prompt to the LLM and returns the model's raw text response.
     *
     * @param prompt the instruction/input prompt
     * @return model's text response
     * @throws Exception if network fails, request times out, or error response is received
     */
    String chat(String prompt) throws Exception;
}
