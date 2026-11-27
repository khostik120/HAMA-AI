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

      const result = await env.AI.run("@cf/zai-org/glm-4.7-flash", {
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: message }
        ],
        max_completion_tokens: 1200
      });

      const reply = result?.response ?? result?.choices?.[0]?.message?.content ?? result?.output_text ?? "";
      if (!reply) return json({ error: "The AI returned an empty response. Please try again." }, 502);
      return json({ reply: String(reply), model: "@cf/zai-org/glm-4.7-flash" });
    } catch (error) {
      return json({ error: error?.message || "The AI service could not complete the request." }, 500);
    }
  }
};
