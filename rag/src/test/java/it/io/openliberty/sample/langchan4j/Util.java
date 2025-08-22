package it.io.openliberty.sample.langchan4j;

public class Util {

	private static String githubApiKey = System.getenv("GITHUB_API_KEY");
	private static String ollamaBaseUrl = System.getenv("OLLAMA_BASE_URL");
	private static String mistralAiApiKey = System.getenv("MISTRAL_AI_API_KEY");

	public static boolean usingGithub() {
		return githubApiKey != null && githubApiKey.startsWith("ghp_");
	}

	public static boolean usingOllama() {
		return ollamaBaseUrl != null && ollamaBaseUrl.startsWith("http");
	}

	public static boolean usingMistralAi() {
		return mistralAiApiKey != null && mistralAiApiKey.length() > 30;
	}

}
