import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (request) => {
  if (request.method !== "DELETE" && request.method !== "POST") {
    return Response.json({ error: "Method not allowed" }, { status: 405 });
  }

  const authorization = request.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) {
    return Response.json({ error: "Authentication required" }, { status: 401 });
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !anonKey || !serviceRoleKey) {
    return Response.json({ error: "Service configuration unavailable" }, { status: 500 });
  }

  const userClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authorization } },
    auth: { persistSession: false },
  });
  const { data, error: userError } = await userClient.auth.getUser();
  if (userError || !data.user) {
    return Response.json({ error: "Invalid session" }, { status: 401 });
  }

  const adminClient = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false },
  });
  const email = data.user.email?.trim().toLowerCase();
  if (email) {
    const { data: cooldown } = await adminClient
      .from("account_deletion_cooldowns")
      .select("deleted_at")
      .eq("email", email)
      .maybeSingle();
    if (cooldown?.deleted_at && Date.now() - new Date(cooldown.deleted_at).getTime() < 24 * 60 * 60 * 1000) {
      return Response.json({ error: "Account deletion is available once every 24 hours." }, { status: 429 });
    }
    await adminClient.from("account_deletion_cooldowns").upsert({ email, deleted_at: new Date().toISOString() });
  }
  const { error: deleteError } = await adminClient.auth.admin.deleteUser(data.user.id);
  if (deleteError) {
    return Response.json({ error: "Account deletion failed" }, { status: 500 });
  }

  return Response.json({ deleted: true });
});
