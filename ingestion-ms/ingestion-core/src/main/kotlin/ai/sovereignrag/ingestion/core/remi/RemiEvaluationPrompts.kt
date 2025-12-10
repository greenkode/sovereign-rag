package ai.sovereignrag.ingestion.core.remi

object RemiEvaluationPrompts {

    val ANSWER_RELEVANCE_PROMPT = """
You are an expert evaluator assessing whether an AI-generated answer is relevant to the user's question.

## Task
Evaluate how well the answer addresses the user's question. Consider:
1. Does the answer directly address what was asked?
2. Is the answer complete and covers the key aspects of the question?
3. Does the answer stay focused on the question without unnecessary tangents?

## Scoring Guidelines
- 1.0: Perfect relevance - answer fully addresses the question
- 0.8-0.9: High relevance - answer addresses the question well with minor gaps
- 0.6-0.7: Moderate relevance - answer partially addresses the question
- 0.4-0.5: Low relevance - answer touches on the topic but misses key points
- 0.2-0.3: Poor relevance - answer barely relates to the question
- 0.0-0.1: No relevance - answer does not address the question at all

## Input
Question: {{QUESTION}}

Answer: {{ANSWER}}

## Output Format
Respond with ONLY a JSON object (no markdown, no explanation outside JSON):
{"score": <float between 0.0 and 1.0>, "reasoning": "<brief explanation>"}
""".trimIndent()

    val CONTEXT_RELEVANCE_PROMPT = """
You are an expert evaluator assessing whether retrieved context chunks are relevant to a user's question.

## Task
Evaluate how relevant the retrieved context is to answering the user's question. Consider:
1. Does the context contain information that helps answer the question?
2. Is the context specific to the question's topic?
3. Would this context be useful for generating an accurate answer?

## Scoring Guidelines
- 1.0: Perfectly relevant - context directly answers or strongly supports answering the question
- 0.8-0.9: Highly relevant - context is very useful for answering the question
- 0.6-0.7: Moderately relevant - context provides some useful information
- 0.4-0.5: Partially relevant - context is tangentially related
- 0.2-0.3: Minimally relevant - context barely relates to the question
- 0.0-0.1: Not relevant - context has nothing to do with the question

## Input
Question: {{QUESTION}}

Retrieved Context:
{{CONTEXT}}

## Output Format
Respond with ONLY a JSON object (no markdown, no explanation outside JSON):
{"score": <float between 0.0 and 1.0>, "reasoning": "<brief explanation>"}
""".trimIndent()

    val GROUNDEDNESS_PROMPT = """
You are an expert evaluator assessing whether an AI-generated answer is grounded in (supported by) the provided context.

## Task
Evaluate whether the claims and information in the answer can be verified from the provided context. Consider:
1. Are all factual claims in the answer supported by the context?
2. Does the answer avoid making claims not present in the context (hallucination)?
3. Is the answer faithful to the source material?

## Scoring Guidelines
- 1.0: Fully grounded - every claim in the answer is supported by the context
- 0.8-0.9: Mostly grounded - nearly all claims are supported, minor additions
- 0.6-0.7: Partially grounded - most claims supported but some unsupported additions
- 0.4-0.5: Weakly grounded - significant claims are not in the context
- 0.2-0.3: Poorly grounded - most of the answer is not supported by context
- 0.0-0.1: Not grounded - answer contradicts or is unrelated to the context (hallucination)

## Input
Context:
{{CONTEXT}}

Answer: {{ANSWER}}

## Output Format
Respond with ONLY a JSON object (no markdown, no explanation outside JSON):
{"score": <float between 0.0 and 1.0>, "reasoning": "<brief explanation>", "hallucination_detected": <true/false>}
""".trimIndent()

    fun buildAnswerRelevancePrompt(question: String, answer: String): String {
        return ANSWER_RELEVANCE_PROMPT
            .replace("{{QUESTION}}", question)
            .replace("{{ANSWER}}", answer)
    }

    fun buildContextRelevancePrompt(question: String, context: String): String {
        return CONTEXT_RELEVANCE_PROMPT
            .replace("{{QUESTION}}", question)
            .replace("{{CONTEXT}}", context)
    }

    fun buildGroundednessPrompt(context: String, answer: String): String {
        return GROUNDEDNESS_PROMPT
            .replace("{{CONTEXT}}", context)
            .replace("{{ANSWER}}", answer)
    }
}
