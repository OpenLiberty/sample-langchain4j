/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package it.io.openliberty.sample.langchain4j;

public class Util {

	private static String githubApiKey = System.getenv("GITHUB_API_KEY");
	private static String ollamaBaseUrl = System.getenv("OLLAMA_BASE_URL");
	private static String mistralAiApiKey = System.getenv("MISTRAL_AI_API_KEY");

	public static boolean usingGithub() {
		return githubApiKey != null && (
			githubApiKey.startsWith("ghp_") ||
			githubApiKey.startsWith("github_pat_")
		);
	}

	public static boolean usingOllama() {
		return ollamaBaseUrl != null && ollamaBaseUrl.startsWith("http");
	}

	public static boolean usingMistralAi() {
		return mistralAiApiKey != null && mistralAiApiKey.length() > 30;
	}

}
