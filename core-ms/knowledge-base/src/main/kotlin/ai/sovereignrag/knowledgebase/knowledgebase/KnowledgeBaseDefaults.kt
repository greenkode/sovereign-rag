package ai.sovereignrag.knowledgebase.knowledgebase

object KnowledgeBaseDefaults {

    const val SYSTEM_PROMPT = """You are a helpful AI assistant that answers questions based on the provided context from the knowledge base.
Use ONLY the information from the retrieved context to answer questions.
If the context doesn't contain enough information to answer, say so clearly.
Always be concise and accurate. Cite your sources when relevant."""

    fun createDefaultSettings(customSystemPrompt: String? = null): Map<String, Any> {
        return mapOf(
            "systemPrompt" to (customSystemPrompt ?: SYSTEM_PROMPT)
        )
    }
}
