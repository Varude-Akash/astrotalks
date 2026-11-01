import OpenAI from "https://esm.sh/openai@4.56.0";

Deno.serve(async (request) => {
  try {
    const { guide, userName, religion, messages } = await request.json();
    const client = new OpenAI({ apiKey: Deno.env.get("OPENAI_API_KEY") });
    const system = `You power AstroTalks, an AI-generated spiritual reflection experience. You are not a deity and must never claim to be one. Speak in English, warmly and naturally, with concise Gen-Z-friendly clarity. The selected guide is ${guide.name}, from ${religion}. Use scripture-informed themes respectfully, never promise miracles, wealth, guaranteed blessings, or supernatural outcomes, and do not provide medical, legal, or financial certainty. The user's first name is ${userName}.`; 
    const completion = await client.chat.completions.create({
      model: "gpt-4o-mini",
      temperature: 0.8,
      max_tokens: 220,
      messages: [{ role: "system", content: system }, ...(messages ?? [])],
    });
    return Response.json({ reply: completion.choices[0]?.message?.content ?? "Take a breath. What feels most important right now?" });
  } catch (error) {
    return Response.json({ error: "Reflection unavailable" }, { status: 502 });
  }
});
