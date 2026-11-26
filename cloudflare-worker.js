export default {
  async fetch(request, env) {
    const cors = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
      "Content-Type": "application/json; charset=utf-8"
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: cors });
    }
    if (request.method !== "POST") {
      return new Response(JSON.stringify({ error: "Use POST /chat" }), {
        status: 405, headers: cors
      });
    }

    try {
      const body = await request.json();
      const message = String(body.message || "").trim();
      if (!message) {
        return new Response(JSON.stringify({ error: "پیغام تش دی." }), {
          status: 400, headers: cors
        });
      }
      if (!env.OPENAI_API_KEY) {
        return new Response(JSON.stringify({ error: "د سرور OpenAI API key نه دی تنظیم شوی." }), {
          status: 500, headers: cors
        });
      }

      const ai = await fetch("https://api.openai.com/v1/responses", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${env.OPENAI_API_KEY}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          model: "gpt-5.5",
          instructions: "You are HAMA AI, a helpful assistant. Reply naturally in Pashto when the user writes Pashto, otherwise use the user's language. Be clear, accurate, and beginner-friendly.",
          input: message,
          max_output_tokens: 700
        })
      });

      const data = await ai.json();
      if (!ai.ok) {
        return new Response(JSON.stringify({
          error: data.error?.message || "OpenAI API error"
        }), { status: ai.status, headers: cors });
      }

      const reply = (data.output || [])
        .filter(item => item.type === "message")
        .flatMap(item => item.content || [])
        .filter(item => item.type === "output_text")
        .map(item => item.text || "")
        .join("\n")
        .trim();

      return new Response(JSON.stringify({
        reply: reply || "بښنه غواړم، دا ځل ځواب ترلاسه نه شو."
      }), { headers: cors });
    } catch (error) {
      return new Response(JSON.stringify({ error: "د غوښتنې په پروسس کې ستونزه رامنځته شوه." }), {
        status: 500, headers: cors
      });
    }
  }
};
