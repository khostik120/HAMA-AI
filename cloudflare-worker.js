const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json; charset=utf-8" },
  });
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    const url = new URL(request.url);
    if (url.pathname !== "/chat") return json({ error: "HAMA AI server is running. Use POST /chat." }, 404);
    if (request.method !== "POST") return json({ error: "Only POST is supported." }, 405);
    if (!env.AI) return json({ error: "Workers AI binding is missing. Add a binding named AI." }, 500);

    try {
      const body = await request.json();
      const message = typeof body.message === "string" ? body.message.trim() : "";
      const language = typeof body.language === "string" ? body.language.trim() : "Auto-detect";
      const history = Array.isArray(body.messages)
        ? body.messages
            .filter((item) =>
              item &&
              (item.role === "user" || item.role === "assistant") &&
              typeof item.content === "string" &&
              item.content.trim()
            )
            .slice(-6)
            .map((item) => ({
              role: item.role,
              content: item.content.trim().slice(0, 1600),
            }))
        : [];

      if (!message) return json({ error: "Please enter a message." }, 400);
      if (message.length > 8000) return json({ error: "Message is too long." }, 413);

      const systemPrompt = [
        "You are HAMA AI, a helpful, accurate, friendly multilingual AI assistant.",
        "You can understand and answer in many languages.",
        language && language !== "Auto-detect"
          ? "The user selected this reply language: " + language + ". Reply in that language."
          : "Detect the language of the latest user message and reply in the same language.",
        "Do not claim to be ChatGPT or OpenAI. Your name is HAMA AI.",
        "Be clear and honest. If you do not know something, say so."
      ].join(" ");

      const result = await env.AI.run("@cf/meta/llama-3.2-3b-instruct", {
        messages: [
          { role: "system", content: systemPrompt },
          ...history,
          ...(history.length > 0 &&
            history[history.length - 1].role === "user" &&
            history[history.length - 1].content === message
              ? []
              : [{ role: "user", content: message }]),
        ],
        max_completion_tokens: 600,
        stream: true
      });

      // Pass the model's token stream straight to Android so the first words
      // appear immediately instead of waiting for the complete answer.
      if (result instanceof ReadableStream) {
        return new Response(result, {
          status: 200,
          headers: {
            ...corsHeaders,
            "Content-Type": "text/event-stream; charset=utf-8",
            "Cache-Control": "no-cache, no-transform",
            "X-Accel-Buffering": "no"
          }
        });
      }
      return json({ error: "The AI streaming response was unavailable." }, 502);
    } catch (error) {
      return json({ error: error?.message || "The AI service could not complete the request." }, 500);
    }
  }
};
